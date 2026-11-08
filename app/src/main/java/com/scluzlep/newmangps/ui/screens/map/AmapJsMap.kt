package com.scluzlep.newmangps.ui.screens.map

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebChromeClient
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.Gson
import com.scluzlep.newmangps.BuildConfig
import com.scluzlep.newmangps.data.util.CoordinateTransform
import com.scluzlep.newmangps.ui.LocalAppLanguage
import org.json.JSONObject

private data class MapPoint(val longitude: Double, val latitude: Double)
data class MapDeviceMarker(
    val teid: String, val name: String, val longitude: Double, val latitude: Double,
    val direction: Int, val status: String, val iconUrl: String = ""
)

@Composable
fun AmapJsMap(
    points: List<CoordinateTransform.LatLng>,
    markerIndex: Int,
    onMapTap: ((CoordinateTransform.LatLng) -> Unit)? = null,
    circleRadiusMeters: Int = 0,
    markerName: String = "",
    markerDirection: Int = 0,
    markerOnline: Boolean = true,
    markerStatus: String = if (markerOnline) "online" else "offline",
    markerIconUrl: String = "",
    followMarker: Boolean = false,
    autoCenter: Boolean = true,
    focusRequest: String = "",
    routeSpeeds: List<Int> = emptyList(),
    controlsTopPx: Int = 12,
    otherMarkers: List<MapDeviceMarker> = emptyList(),
    polygonPoints: List<CoordinateTransform.LatLng> = emptyList(),
    onDeviceTap: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val labels = mapOf(
        "error" to language.text("MapLoadFailed", "地图加载失败，请检查网络后返回重试"),
        "traffic" to language.text("RoadTraffic", "交通"),
        "satellite" to language.text("Satellite", "卫星"),
        "standard" to language.text("StandardMap", "标准"),
        "start" to language.text("StartLocation", "起点"),
        "end" to language.text("EndLocation", "终点")
    )
    key(language.code, labels) { AndroidView(
        factory = { context -> AmapMapView(context, onMapTap, onDeviceTap, controlsTopPx, labels) },
        update = {
            it.setOnMapTap(onMapTap)
            it.setOnDeviceTap(onDeviceTap)
            it.updateMap(points, markerIndex, circleRadiusMeters, markerName, markerDirection, markerOnline, markerStatus, markerIconUrl, followMarker, autoCenter, focusRequest, routeSpeeds, otherMarkers, polygonPoints)
        },
        modifier = modifier
    ) }
}

@SuppressLint("SetJavaScriptEnabled")
private class AmapMapView(
    context: android.content.Context,
    private var onMapTap: ((CoordinateTransform.LatLng) -> Unit)?,
    private var onDeviceTap: ((String) -> Unit)?,
    private val controlsTopPx: Int,
    private val labels: Map<String, String>
) : WebView(context) {
    private var pageReady = false
    private var pendingJson = "[]"
    private var pendingIndex = 0
    private var pendingRadius = 0
    private var pendingName = ""
    private var pendingDirection = 0
    private var pendingOnline = true
    private var pendingStatus = "online"
    private var pendingIconUrl = ""
    private var pendingFollowMarker = false
    private var pendingSpeeds = "[]"
    private var pendingOtherMarkers = "[]"
    private var pendingPolygon = "[]"
    private var allowedTeids = emptySet<String>()
    private var previousRoute = ""
    private var previousOtherMarkers = "[]"
    private var lastPayload = ""
    private var hasFitted = false
    private var previousFocusRequest = ""
    private var previousFence = ""

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
                if (longitude in -180.0..180.0 && latitude in -85.0..85.0) {
                    post { onMapTap?.invoke(CoordinateTransform.LatLng(latitude, longitude)) }
                }
            }
            @JavascriptInterface
            fun onDeviceTap(teid: String) {
                if (teid in allowedTeids) post { onDeviceTap?.invoke(teid) }
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

    fun setOnDeviceTap(callback: ((String) -> Unit)?) {
        onDeviceTap = callback
    }

    fun updateMap(
        points: List<CoordinateTransform.LatLng>, markerIndex: Int, circleRadiusMeters: Int,
        markerName: String, markerDirection: Int, markerOnline: Boolean, markerStatus: String,
        markerIconUrl: String,
        followMarker: Boolean,
        autoCenter: Boolean,
        focusRequest: String,
        routeSpeeds: List<Int>,
        otherMarkers: List<MapDeviceMarker>, polygonPoints: List<CoordinateTransform.LatLng>
    ) {
        val valid = points.filter {
            it.latitude in -85.0..85.0 && it.longitude in -180.0..180.0
        }
        val routeJson = Gson().toJson(valid.map { MapPoint(it.longitude, it.latitude) })
        val index = markerIndex.coerceIn(0, (valid.size - 1).coerceAtLeast(0))
        val otherJson = Gson().toJson(otherMarkers)
        val polygonJson = Gson().toJson(polygonPoints.map { MapPoint(it.longitude, it.latitude) })
        val speedsJson = Gson().toJson(routeSpeeds)
        val payload = listOf(routeJson, index.toString(), circleRadiusMeters.toString(), markerName,
            markerDirection.toString(), markerOnline.toString(), markerStatus, markerIconUrl,
            followMarker.toString(), autoCenter.toString(), focusRequest, speedsJson, otherJson, polygonJson)
            .joinToString("\u001f")
        if (payload == lastPayload) return
        lastPayload = payload
        val routeChanged = routeJson != previousRoute
        val markersChanged = otherJson != previousOtherMarkers
        val focusChanged = focusRequest != previousFocusRequest
        val fence = "$circleRadiusMeters:$polygonJson"
        val fenceChanged = fence != previousFence
        previousFocusRequest = focusRequest
        previousFence = fence
        previousRoute = routeJson
        previousOtherMarkers = otherJson
        pendingJson = routeJson
        pendingIndex = index
        pendingRadius = circleRadiusMeters.coerceAtLeast(0)
        pendingName = markerName
        pendingDirection = markerDirection
        pendingOnline = markerOnline
        pendingStatus = markerStatus
        pendingIconUrl = markerIconUrl
        pendingFollowMarker = followMarker
        pendingSpeeds = speedsJson
        allowedTeids = otherMarkers.map { it.teid }.toSet()
        pendingOtherMarkers = otherJson
        pendingPolygon = polygonJson
        if (pageReady) {
            val hasContent = valid.isNotEmpty() || otherMarkers.isNotEmpty()
            applyMapData(fitRoute = hasContent &&
                (!hasFitted || focusChanged || fenceChanged || autoCenter && (routeChanged || markersChanged)),
                routeChanged = routeChanged, focusPrimary = hasFitted && focusChanged)
            if (hasContent) hasFitted = true
        }
    }

    private fun applyMapData(fitRoute: Boolean = true, routeChanged: Boolean = true, focusPrimary: Boolean = false) {
        val script = "if (window.setMapData) window.setMapData(" +
            JSONObject.quote(pendingJson) + "," + pendingIndex + "," + fitRoute + "," + routeChanged + "," + pendingRadius + "," +
            JSONObject.quote(pendingName) + "," + pendingDirection + "," + pendingOnline + "," + JSONObject.quote(pendingStatus) + "," + JSONObject.quote(pendingIconUrl) + "," + pendingFollowMarker + "," + JSONObject.quote(pendingSpeeds) + "," +
            JSONObject.quote(pendingOtherMarkers) + "," + JSONObject.quote(pendingPolygon) + "," + focusPrimary + ");"
        evaluateJavascript(script, null)
    }

    private fun buildMapHtml(): String {
        val webKey = BuildConfig.AMAP_WEB_KEY
        val securityCode = BuildConfig.AMAP_SECURITY_CODE
        val quotedSecurityCode = Gson().toJson(securityCode)
        val quotedLabels = Gson().toJson(labels)
        return """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
          <meta charset="utf-8">
          <style>
            html,body,#map{width:100%;height:100%;padding:0;margin:0;overflow:hidden}
            body{background:#f1f4f6}
            #traffic{position:absolute;z-index:10;top:${controlsTopPx}px;left:12px;border:0;border-radius:4px;background:#fff;color:#24292f;padding:8px 12px;font:14px sans-serif;box-shadow:0 1px 4px #0003}
            #traffic.active{background:#1677ff;color:#fff}
            #layer{position:absolute;z-index:10;top:${controlsTopPx}px;left:78px;border:0;border-radius:4px;background:#fff;color:#24292f;padding:8px 12px;font:14px sans-serif;box-shadow:0 1px 4px #0003}
            .device-marker{display:flex;align-items:center;gap:5px;white-space:nowrap;font:12px sans-serif;color:#202b36}
            .device-arrow{width:24px;height:24px;display:grid;place-items:center;border:2px solid white;border-radius:50%;background:#148047;color:white;box-shadow:0 1px 5px #0006;font-size:16px}
            .device-marker.offline .device-arrow{background:#8b949e}
            .device-marker.moving .device-arrow{background:#1677ff}
            .device-marker.alarm .device-arrow{background:#db3f44}
            .device-marker.sleep .device-arrow{background:#d48d18}
            .device-arrow img{width:20px;height:20px;object-fit:contain;transform-origin:center}
            .device-name{max-width:130px;overflow:hidden;text-overflow:ellipsis;background:#fff;border:1px solid #d8dfe3;border-radius:3px;padding:4px 6px;box-shadow:0 1px 4px #0003}
            #map-error{display:none;position:absolute;z-index:20;inset:0;background:#f1f4f6;color:#a52332;align-items:center;justify-content:center;font:14px sans-serif;text-align:center;padding:24px}
          </style>
          <script>window._AMapSecurityConfig={securityJsCode:$quotedSecurityCode};</script>
          <script>window.addEventListener('error',function(){const error=document.getElementById('map-error');if(error)error.style.display='flex';});</script>
          <script src="https://webapi.amap.com/maps?v=2.0&key=$webKey&plugin=AMap.Scale,AMap.ToolBar"></script>
        </head>
        <body>
          <div id="map"></div>
          <div id="map-error">地图加载失败，请检查网络后返回重试</div>
          <button id="traffic" onclick="toggleTraffic(this)">交通</button>
          <button id="layer" onclick="toggleLayer(this)">卫星</button>
          <script>
            const labels=$quotedLabels;
            document.getElementById('map-error').textContent=labels.error;
            document.getElementById('traffic').textContent=labels.traffic;
            document.getElementById('layer').textContent=labels.satellite;
            const map=new AMap.Map('map',{zoom:15,viewMode:'2D',resizeEnable:true,showIndoorMap:true});
            map.addControl(new AMap.Scale());
            map.addControl(new AMap.ToolBar({position:{top:'${controlsTopPx}px',right:'12px'}}));
            const satellite=new AMap.TileLayer.Satellite();
            const roadNet=new AMap.TileLayer.RoadNet();
            let satelliteEnabled=false;
            window.toggleLayer=function(button){
              satelliteEnabled=!satelliteEnabled;
              if(satelliteEnabled){map.add(satellite);map.add(roadNet);}else{map.remove(roadNet);map.remove(satellite);}
              button.textContent=satelliteEnabled?labels.standard:labels.satellite;
            };
            const trafficLayer=new AMap.TileLayer.Traffic({zIndex:10});
            let trafficEnabled=false;
            window.toggleTraffic=function(button){
              trafficEnabled=!trafficEnabled;
              if(trafficEnabled)map.add(trafficLayer);else map.remove(trafficLayer);
              button.classList.toggle('active',trafficEnabled);
            };
            let routeLines=[],marker=null,fenceCircle=null,fencePolygon=null,deviceMarkers=[],endMarkers=[];
            map.on('click',function(event){
              if(window.NativeMap)NativeMap.onMapTap(event.lnglat.getLng(),event.lnglat.getLat());
            });
            window.setMapData=function(serialized,index,fitRoute,routeChanged,radius,name,direction,online,status,iconUrl,followMarker,speedsJson,otherJson,polygonJson,focusPrimary){
              const points=JSON.parse(serialized||'[]').filter(p=>
                Number.isFinite(p.longitude)&&Number.isFinite(p.latitude)&&
                p.longitude>=-180&&p.longitude<=180&&p.latitude>=-85&&p.latitude<=85);
              if(marker){map.remove(marker);marker=null;}
              if(deviceMarkers.length){map.remove(deviceMarkers);deviceMarkers=[];}
              if(fenceCircle){map.remove(fenceCircle);fenceCircle=null;}
              if(fencePolygon){map.remove(fencePolygon);fencePolygon=null;}
              if(routeChanged&&routeLines.length){map.remove(routeLines);routeLines=[];}
              if(routeChanged&&endMarkers.length){map.remove(endMarkers);endMarkers=[];}
              if(routeChanged&&points.length>1){
                const speeds=JSON.parse(speedsJson||'[]');
                for(let i=1;i<points.length;i++){
                  const speed=Number(speeds[i-1]||0);
                  const color=speed<20?'#2d8cf0':speed<40?'#5cadff':speed<60?'#19be6b':speed<80?'#99CC33':speed<100?'#ff9900':speed<120?'#ed4014':'#990033';
                  routeLines.push(new AMap.Polyline({path:[[points[i-1].longitude,points[i-1].latitude],[points[i].longitude,points[i].latitude]],strokeColor:color,strokeWeight:5,strokeOpacity:0.9,showDir:true}));
                }
                map.add(routeLines);
                [0,points.length-1].forEach((pointIndex,endpoint)=>{
                  const label=document.createElement('span');label.className='device-name';label.textContent=endpoint?labels.end:labels.start;
                  const point=points[pointIndex];
                  endMarkers.push(new AMap.Marker({position:[point.longitude,point.latitude],content:label,anchor:'bottom-center'}));
                });
                map.add(endMarkers);
              }
              const path=points.map(p=>new AMap.LngLat(p.longitude,p.latitude));
              const current=points[Math.max(0,Math.min(index,points.length-1))];
              if(current){
              const label=document.createElement('div');
              label.className='device-marker '+(status||'online');
              const arrow=document.createElement('span');
              arrow.className='device-arrow';arrow.textContent='▲';
              if(iconUrl){const image=document.createElement('img');image.src=iconUrl;image.onerror=function(){arrow.textContent='▲';};arrow.textContent='';arrow.appendChild(image);}
              arrow.style.transform='rotate('+Number(direction||0)+'deg)';
              label.appendChild(arrow);
              if(name){const title=document.createElement('span');title.className='device-name';title.textContent=name;label.appendChild(title);}
              marker=new AMap.Marker({position:[current.longitude,current.latitude],anchor:'bottom-center',content:label});
              map.add(marker);
              }
              const others=JSON.parse(otherJson||'[]');
              others.forEach(device=>{
                if(!Number.isFinite(device.longitude)||!Number.isFinite(device.latitude))return;
                const element=document.createElement('div');
                element.className='device-marker '+(device.status||'offline');
                const arrow=document.createElement('span');arrow.className='device-arrow';arrow.textContent='▲';
                if(device.iconUrl){const image=document.createElement('img');image.src=device.iconUrl;image.onerror=function(){arrow.textContent='▲';};arrow.textContent='';arrow.appendChild(image);}
                arrow.style.transform='rotate('+Number(device.direction||0)+'deg)';
                const title=document.createElement('span');title.className='device-name';title.textContent=device.name||device.teid;
                element.append(arrow,title);
                const item=new AMap.Marker({position:[device.longitude,device.latitude],anchor:'bottom-center',content:element});
                item.on('click',()=>{if(window.NativeMap)NativeMap.onDeviceTap(device.teid);});
                deviceMarkers.push(item);
              });
              if(deviceMarkers.length)map.add(deviceMarkers);
              if(radius>0&&current){
                fenceCircle=new AMap.Circle({center:[current.longitude,current.latitude],radius,
                  strokeColor:'#e5484d',strokeWeight:2,strokeOpacity:0.9,fillColor:'#e5484d',fillOpacity:0.12});
                map.add(fenceCircle);
              }
              const vertices=JSON.parse(polygonJson||'[]');
              if(vertices.length>0){
                fencePolygon=new AMap.Polygon({path:vertices.map(p=>[p.longitude,p.latitude]),
                  strokeColor:'#e5484d',strokeWeight:2,fillColor:'#e5484d',fillOpacity:0.12});
                map.add(fencePolygon);
              }
              if(focusPrimary&&current){
                map.setZoomAndCenter(Math.max(map.getZoom(),15),[current.longitude,current.latitude]);
              }else if(fitRoute){
                if(path.length===1&&deviceMarkers.length===0&&!fenceCircle&&!fencePolygon)map.setCenter(path[0]);
                else {const overlays=[...routeLines,marker,...deviceMarkers,fenceCircle,fencePolygon].filter(Boolean);if(overlays.length)map.setFitView(overlays,false,[48,32,220,32]);}
              }else if(current&&followMarker&&!map.getBounds().contains(new AMap.LngLat(current.longitude,current.latitude))){
                map.setCenter([current.longitude,current.latitude]);
              }
            };
          </script>
        </body>
        </html>
        """.trimIndent()
    }
}
