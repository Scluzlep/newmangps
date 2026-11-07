package com.scluzlep.newmangps.ui.screens.map

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebChromeClient
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.Gson
import com.scluzlep.newmangps.BuildConfig
import com.scluzlep.newmangps.data.util.CoordinateTransform
import org.json.JSONObject

private data class MapPoint(val longitude: Double, val latitude: Double)

@Composable
fun AmapJsMap(
    points: List<CoordinateTransform.LatLng>,
    markerIndex: Int,
    onMapTap: ((CoordinateTransform.LatLng) -> Unit)? = null,
    circleRadiusMeters: Int = 0,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context -> AmapMapView(context, onMapTap) },
        update = {
            it.setOnMapTap(onMapTap)
            it.updateMap(points, markerIndex, circleRadiusMeters)
        },
        modifier = modifier
    )
}

@SuppressLint("SetJavaScriptEnabled")
private class AmapMapView(
    context: android.content.Context,
    private var onMapTap: ((CoordinateTransform.LatLng) -> Unit)?
) : WebView(context) {
    private var pageReady = false
    private var pendingJson = "[]"
    private var pendingIndex = 0
    private var pendingRadius = 0
    private var previousRoute = ""

    init {
        setBackgroundColor(Color.rgb(241, 244, 246))
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.defaultTextEncodingName = "UTF-8"
        addJavascriptInterface(object {
            @JavascriptInterface
            fun onMapTap(longitude: Double, latitude: Double) {
                post { onMapTap?.invoke(CoordinateTransform.LatLng(latitude, longitude)) }
            }
        }, "NativeMap")
        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                pageReady = true
                applyMapData()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: android.webkit.WebResourceRequest
            ): Boolean = request.isForMainFrame && request.url.host != "app.newsmy-nmgps.com"
        }
        loadDataWithBaseURL(
            "https://app.newsmy-nmgps.com/",
            buildMapHtml(),
            "text/html",
            "UTF-8",
            null
        )
    }

    fun setOnMapTap(callback: ((CoordinateTransform.LatLng) -> Unit)?) {
        onMapTap = callback
    }

    fun updateMap(points: List<CoordinateTransform.LatLng>, markerIndex: Int, circleRadiusMeters: Int) {
        val valid = points.filter {
            it.latitude in -85.0..85.0 && it.longitude in -180.0..180.0
        }
        val routeJson = Gson().toJson(valid.map { MapPoint(it.longitude, it.latitude) })
        val index = markerIndex.coerceIn(0, (valid.size - 1).coerceAtLeast(0))
        val routeChanged = routeJson != previousRoute
        previousRoute = routeJson
        pendingJson = routeJson
        pendingIndex = index
        pendingRadius = circleRadiusMeters.coerceAtLeast(0)
        if (pageReady) applyMapData(routeChanged = routeChanged)
    }

    private fun applyMapData(fitRoute: Boolean = true, routeChanged: Boolean = true) {
        val script = "if (window.setMapData) window.setMapData(" +
            JSONObject.quote(pendingJson) + "," + pendingIndex + "," + fitRoute + "," + routeChanged + "," + pendingRadius + ");"
        evaluateJavascript(script, null)
    }

    private fun buildMapHtml(): String {
        val webKey = BuildConfig.AMAP_WEB_KEY
        val securityCode = BuildConfig.AMAP_SECURITY_CODE
        val quotedSecurityCode = Gson().toJson(securityCode)
        return """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
          <meta charset="utf-8">
          <style>
            html,body,#map{width:100%;height:100%;padding:0;margin:0;overflow:hidden}
            body{background:#f1f4f6}
            #traffic{position:absolute;z-index:10;top:12px;left:12px;border:0;border-radius:4px;background:#fff;color:#24292f;padding:8px 12px;font:14px sans-serif;box-shadow:0 1px 4px #0003}
            #traffic.active{background:#1677ff;color:#fff}
          </style>
          <script>window._AMapSecurityConfig={securityJsCode:$quotedSecurityCode};</script>
          <script src="https://webapi.amap.com/maps?v=2.0&key=$webKey&plugin=AMap.Scale,AMap.ToolBar,AMap.MapType"></script>
        </head>
        <body>
          <div id="map"></div>
          <button id="traffic" onclick="toggleTraffic(this)">交通</button>
          <script>
            const map=new AMap.Map('map',{zoom:12,viewMode:'2D',resizeEnable:true});
            map.addControl(new AMap.Scale());
            map.addControl(new AMap.ToolBar({position:{top:'12px',right:'12px'}}));
            map.addControl(new AMap.MapType({defaultType:0,showRoad:true}));
            const trafficLayer=new AMap.TileLayer.Traffic({zIndex:10});
            let trafficEnabled=false;
            window.toggleTraffic=function(button){
              trafficEnabled=!trafficEnabled;
              if(trafficEnabled)map.add(trafficLayer);else map.remove(trafficLayer);
              button.classList.toggle('active',trafficEnabled);
            };
            let routeLine=null,marker=null,fenceCircle=null;
            map.on('click',function(event){
              if(window.NativeMap)NativeMap.onMapTap(event.lnglat.getLng(),event.lnglat.getLat());
            });
            window.setMapData=function(serialized,index,fitRoute,routeChanged,radius){
              const points=JSON.parse(serialized||'[]').filter(p=>
                Number.isFinite(p.longitude)&&Number.isFinite(p.latitude)&&
                p.longitude>=-180&&p.longitude<=180&&p.latitude>=-85&&p.latitude<=85);
              if(marker){map.remove(marker);marker=null;}
              if(fenceCircle){map.remove(fenceCircle);fenceCircle=null;}
              if(routeChanged&&routeLine){map.remove(routeLine);routeLine=null;}
              if(routeChanged&&points.length>1){
                const routePath=points.map(p=>new AMap.LngLat(p.longitude,p.latitude));
                routeLine=new AMap.Polyline({path:routePath,strokeColor:'#1677ff',strokeWeight:5,strokeOpacity:0.9});
                map.add(routeLine);
              }
              if(points.length===0)return;
              const path=points.map(p=>new AMap.LngLat(p.longitude,p.latitude));
              const current=points[Math.max(0,Math.min(index,points.length-1))];
              marker=new AMap.Marker({position:[current.longitude,current.latitude],anchor:'bottom-center'});
              map.add(marker);
              if(radius>0){
                fenceCircle=new AMap.Circle({center:[current.longitude,current.latitude],radius,
                  strokeColor:'#e5484d',strokeWeight:2,strokeOpacity:0.9,fillColor:'#e5484d',fillOpacity:0.12});
                map.add(fenceCircle);
              }
              if(fitRoute){
                if(path.length===1)map.setCenter(path[0]);
                else map.setFitView(routeLine?[routeLine,marker]:[marker],false,[64,48,280,48]);
              }else{
                map.setCenter(path[Math.max(0,Math.min(index,path.length-1))]);
              }
            };
          </script>
        </body>
        </html>
        """.trimIndent()
    }
}
