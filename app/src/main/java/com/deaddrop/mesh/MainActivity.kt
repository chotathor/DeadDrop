package com.deaddrop.mesh

import android.Manifest; import android.bluetooth.*; import android.bluetooth.le.*
import android.content.*; import android.content.pm.*; import android.graphics.*; import android.graphics.drawable.*
import android.location.*; import android.os.*; import android.view.*; import android.view.animation.*; import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat; import androidx.core.content.ContextCompat
import java.util.*; import java.util.concurrent.*

class MainActivity : AppCompatActivity() {
    val bg=0xFF0d0d12.toInt();val cb=0xFF14141c.toInt();val grn=0xFF00d684.toInt();val red=0xFFff3355.toInt();val wh=0xFFf4f4f6.toInt();val gr=0xFF6b6b7b.toInt();val faint=0xFF3a3a44.toInt()
    val devices=ConcurrentHashMap<String,BleDev>();var scanCnt=0;var scanner:BluetoothLeScanner?=null
    var tab=0;var lat=0.0;var lng=0.0;var targetRssi=0;var targetAddr=""
    val favs=mutableSetOf<String>();lateinit var feed:LinearLayout;lateinit var statusLine:TextView;lateinit var countPill:TextView;lateinit var tabBar:LinearLayout
    data class BleDev(val addr:String,var name:String,var rssi:Int,var firstSeen:Long,var lastSeen:Long,var count:Int,val services:List<String>)
    // Chat
    val CHAT=UUID.fromString("0000cafe-0000-1000-8000-00805f9b34fb");val CHAT_CHR=UUID.fromString("0000beef-0000-1000-8000-00805f9b34fb")
    var gattServer:BluetoothGattServer?=null;var gatt:BluetoothGatt?=null;var chatPeer:String?=null
    val chatMsgs=mutableListOf<Pair<Boolean,String>>()

    override fun onCreate(s:Bundle?){super.onCreate(s);window.statusBarColor=bg;window.navigationBarColor=bg
        loadFavs()
        if(!check()){val v=tv("BLE Locator",wh,22f,true).apply{gravity=Gravity.CENTER;setPadding(40,100,40,8)};val sub=tv("Grant permissions to start scanning",gr,13f,false).apply{gravity=Gravity.CENTER}
            val ll=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(40,0,40,0);addView(v);addView(sub)};setContentView(ll);return}
        build();startScan();startLoc();startChatServer()}

    fun check():Boolean{val p=listOf(Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.ACCESS_FINE_LOCATION);val n=p.filter{checkSelfPermission(it)!=PackageManager.PERMISSION_GRANTED};if(n.isNotEmpty()){requestPermissions(n.toTypedArray(),42);return false};return true}

    fun build(){val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(0,32,0,0)}
        // HEADER
        val hdr=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(20,16,20,12);gravity=Gravity.CENTER_VERTICAL}
        val title=tv("BLE Locator",wh,20f,true).apply{layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
        hdr.addView(title)
        countPill=tv("0",grn,13f,true).apply{setBackgroundDrawable(pill(grn));setPadding(14,4,14,4)};hdr.addView(countPill);root.addView(hdr)
        // TABS
        tabBar=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(16,0,16,0)}
        for((i,it) in listOf("Scan" to "\uD83D\uDCED","Locate" to "\uD83C\uDFAF","Map" to "\uD83D\uDDFA\uFE0F","Chat" to "\uD83D\uDCAC","More" to "\u22EE").withIndex()){val t=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
            t.addView(tv(it.second,grn,20f,false).apply{gravity=Gravity.CENTER});t.addView(tv(it.first,grn,10f,false).apply{gravity=Gravity.CENTER;setPadding(0,dp(2),0,0)})
            val u=View(this).apply{setBackgroundColor(grn);layoutParams=LinearLayout.LayoutParams(dp(20),dp(2)).apply{gravity=Gravity.CENTER;setMargins(0,dp(6),0,0)};visibility=if(i==0)View.VISIBLE else View.INVISIBLE}
            t.addView(u);t.setPadding(0,dp(6),0,dp(8));t.setOnClickListener{selectTab(i)};tabBar.addView(t)}
        root.addView(tabBar)
        // STATUS
        statusLine=tv("\u25CF Scanning for devices...",grn,12f,false).apply{setPadding(20,4,20,10)};root.addView(statusLine)
        // CONTENT
        feed=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(0,4,0,100)};root.addView(ScrollView(this).apply{addView(feed)})
        setContentView(root);selectTab(0)}

    fun selectTab(i:Int){tab=i
        for(j in 0 until tabBar.childCount){val g=tabBar.getChildAt(j) as LinearLayout;g.alpha=if(j==i)1f else 0.35f
            val u=g.getChildAt(2) as View;u.visibility=if(j==i)View.VISIBLE else View.INVISIBLE}
        show()}

    fun show(){feed.removeAllViews();val all=devices.values.sortedByDescending{it.rssi}
        when(tab){0->{all.take(80).forEach{addDev(it)};if(all.isEmpty())empty("No devices found\nTurn on Bluetooth on nearby phones")}
            1->{locateMode()};2->{showMap()};3->{showChat()};4->{showStats(all)}};countPill.text="${devices.size}"}

    fun addDev(d:BleDev){val card=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(20,14,20,14);setBackgroundDrawable(cardBg());layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,3,16,3)}}
        val sig=when{d.rssi>-50->grn;d.rssi>-70->0xFF88aa00.toInt();d.rssi>-85->0xFFff8800.toInt();else->red}
        val strip=View(this).apply{setBackgroundColor(sig);layoutParams=LinearLayout.LayoutParams(dp(3),-1).apply{setMargins(0,0,dp(14),0)}}
        card.addView(strip)
        val info=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
        val oui=getOUI(d.addr);val name=if(d.name=="Unknown")"$oui Device" else "$oui ${d.name}".trim().take(32)
        info.addView(tv(name,wh,14f,false))
        val dist=when{d.rssi>-40->"<1m";d.rssi>-55->"~3m";d.rssi>-70->"~10m";d.rssi>-85->"~20m";else->"50m+"}
        info.addView(tv("$dist  \u2022  ${d.rssi} dBm  \u2022  \u00D7${d.count}",gr,11f,false).apply{setPadding(0,dp(2),0,0)})
        card.addView(info)
        val favIcon=tv(if(favs.contains(d.addr))"\u2605" else "\u2606",if(favs.contains(d.addr))0xFFFFAA00.toInt() else gr,22f,false).apply{setPadding(dp(8),0,0,0)}
        favIcon.setOnClickListener{if(favs.contains(d.addr))favs.remove(d.addr)else favs.add(d.addr);saveFavs();show()};card.addView(favIcon)
        card.setOnClickListener{targetAddr=d.addr;targetRssi=d.rssi;selectTab(1)};feed.addView(card)}

    fun locateMode(){feed.removeAllViews()
        card(feed,tv("\uD83C\uDFAF Locate Device",wh,18f,true))
        if(targetAddr.isNotEmpty()&&devices.containsKey(targetAddr)){val d=devices[targetAddr]!!
            val oui=getOUI(d.addr);val name=if(d.name=="Unknown")"$oui Device" else d.name.take(28)
            tv("Tracking: $name",grn,16f,true).apply{setPadding(20,8,20,4)}.also{feed.addView(it)}
            val dist=when{d.rssi>-40->"Very close (<1m)";d.rssi>-55->"Getting warm (~3m)";d.rssi>-70->"Nearby (~10m)";d.rssi>-85->"In range (~20m)";else->"Far away (50m+)"}
            val w=((d.rssi+100)*3).coerceIn(20,300);val clr=when{d.rssi>-50->grn;d.rssi>-70->0xFF88aa00.toInt();else->red}
            card(feed,LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}.also{c->
                c.addView(tv("$dist | ${d.rssi} dBm",clr,15f,false).apply{setPadding(16,12,16,4)})
                c.addView(View(this).apply{setBackgroundColor(clr);layoutParams=LinearLayout.LayoutParams(w,dp(8))})})}else{tv("Select a device in Scan tab",gr,13f,false).apply{gravity=Gravity.CENTER;setPadding(0,40,0,40)}.also{feed.addView(it)}}}

    fun showMap(){card(feed,tv("\uD83D\uDDFA\uFE0F Proximity Map",wh,18f,true))
        val sorted=devices.values.sortedByDescending{it.rssi}.take(25)
        if(sorted.isEmpty()){tv("Walk around to map devices",gr,13f,false).apply{gravity=Gravity.CENTER;setPadding(0,30,0,30)}.also{feed.addView(it)};return}
        sorted.forEach{d->val w=((d.rssi+100)*2).coerceIn(40,280);val clr=when{d.rssi>-50->grn;d.rssi>-70->0xFF88aa00.toInt();else->red}
            val dist=when{d.rssi>-40->"<1m";d.rssi>-55->"3m";d.rssi>-70->"10m";else->"50m+"}
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(20,4,20,4);gravity=Gravity.CENTER_VERTICAL}
            row.addView(tv(d.name.take(18).padEnd(18),wh,11f,false).apply{setTypeface(Typeface.MONOSPACE)})
            row.addView(View(this).apply{setBackgroundColor(clr);layoutParams=LinearLayout.LayoutParams(w,dp(6)).apply{setMargins(dp(8),0,0,0)}})
            row.addView(tv(" $dist",gr,10f,false));feed.addView(row)}}

    fun showChat(){card(feed,tv("\uD83D\uDCAC Mesh Chat",wh,18f,true))
        card(feed,tv("BLE peer-to-peer \u2014 no internet needed",gr,12f,false))
        val inp=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(20,4,20,4)}
        val et=EditText(this).apply{hint="Message...";setHintTextColor(gr);setTextColor(wh);setBackgroundDrawable(cardBg());setPadding(16,12,16,12);textSize=14f;layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
        inp.addView(et);inp.addView(tv("SEND",grn,14f,true).apply{setPadding(16,12,12,12);setOnClickListener{sendChat(et.text.toString().trim());et.text.clear()}})
        feed.addView(inp)
        chatMsgs.take(30).forEach{(me,m)->val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,8,20,8);setBackgroundDrawable(if(me)pill(grn) else cardBg());layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,2,16,2)}}
            b.addView(tv(if(me)"You" else "Peer",if(me)grn else gr,11f,true));b.addView(tv(m,wh,14f,false).apply{setPadding(0,2,0,0)});feed.addView(b)}
        card(feed,tv("Nearby Peers",gr,11f,true))
        peers().forEach{p->val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(20,12,20,12);setBackgroundDrawable(cardBg());layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,2,16,2)}}
            row.addView(tv(p.name.take(20),wh,13f,false).apply{layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
            row.addView(tv("CONNECT",grn,12f,true).apply{setPadding(12,4,12,4);setOnClickListener{connectChat(p.addr)}});feed.addView(row)}
        if(chatMsgs.isEmpty())tv("No messages yet\nConnect to a peer to start",gr,12f,false).apply{gravity=Gravity.CENTER;setPadding(0,20,0,20)}.also{feed.addView(it)}}

    fun showStats(all:List<BleDev>){val u=all.distinctBy{it.name}.size;val a=all.map{it.rssi}.average().toInt()
        card(feed,tv("Statistics",wh,18f,true))
        card(feed,LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}.also{c->
            c.addView(tv("Devices: ${devices.size}  Unique: $u  Scans: $scanCnt",wh,14f,false).apply{setPadding(12,4,12,4)})
            c.addView(tv("Avg Signal: $a dBm  GPS: ${"%.4f".format(lat)},${"%.4f".format(lng)}",wh,14f,false).apply{setPadding(12,4,12,4)})
            c.addView(tv("Favorites: ${favs.size}  Active: ${devices.values.count{favs.contains(it.addr)}}",wh,14f,false).apply{setPadding(12,4,12,4)})})
        card(feed,tv("Export Scan Report",grn,15f,true).apply{setOnClickListener{exportScan()}})
        tv("BLE Locator v7  \u2022  OUI  \u2022  Chat  \u2022  Map",gr,10f,false).apply{gravity=Gravity.CENTER;setPadding(0,12,0,12)}.also{feed.addView(it)}}

    fun startChatServer(){val bt=getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val chr=BluetoothGattCharacteristic(CHAT_CHR,BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE)
        chr.addDescriptor(BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE))
        val svc=BluetoothGattService(CHAT,BluetoothGattService.SERVICE_TYPE_PRIMARY);svc.addCharacteristic(chr)
        gattServer=bt.openGattServer(this,object:BluetoothGattServerCallback(){
            override fun onCharacteristicWriteRequest(d:BluetoothDevice,id:Int,c:BluetoothGattCharacteristic,prep:Boolean,resp:Boolean,off:Int,v:ByteArray){val m=String(v);chatMsgs.add(Pair(false,m));runOnUiThread{if(tab==3)show()};if(resp)gattServer?.sendResponse(d,id,BluetoothGatt.GATT_SUCCESS,0,null)}
            override fun onConnectionStateChange(d:BluetoothDevice,s:Int,ns:Int){if(ns==BluetoothProfile.STATE_DISCONNECTED)chatPeer=null}})
        try{gattServer?.addService(svc)}catch(e:Exception){}}

    fun connectChat(addr:String){val bt=getSystemService(Context.BLUETOOTH_SERVICE)as BluetoothManager;chatPeer=addr
        gatt=bt.adapter.getRemoteDevice(addr).connectGatt(this,false,object:BluetoothGattCallback(){
            override fun onConnectionStateChange(g:BluetoothGatt,s:Int,ns:Int){if(ns==BluetoothProfile.STATE_CONNECTED){g.discoverServices();statusLine.text="\u25CF Chat connected"}}
            override fun onServicesDiscovered(g:BluetoothGatt,s:Int){g.getService(CHAT)?.getCharacteristic(CHAT_CHR)?.let{g.setCharacteristicNotification(it,true);it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))?.let{d->d.value=BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;g.writeDescriptor(d)}}}
            override fun onCharacteristicChanged(g:BluetoothGatt,c:BluetoothGattCharacteristic){val m=String(c.value);chatMsgs.add(Pair(false,m));runOnUiThread{if(tab==3)show()}}},BluetoothDevice.TRANSPORT_LE)}

    fun sendChat(m:String){if(m.isEmpty())return;chatMsgs.add(Pair(true,m))
        gatt?.let{it.getService(CHAT)?.getCharacteristic(CHAT_CHR)?.let{c->c.value=m.toByteArray();it.writeCharacteristic(c)}};if(tab==3)show()}

    fun startScan(){val a=BluetoothAdapter.getDefaultAdapter();if(a==null||!a.isEnabled){statusLine.text="\u26A0 Bluetooth is OFF";return}
        val lm=getSystemService(Context.LOCATION_SERVICE) as LocationManager;if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)&&!lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){statusLine.text="\u26A0 Location is OFF";return}
        scanner=a.bluetoothLeScanner;if(scanner==null){statusLine.text="\u26A0 BLE not supported";return}
        try{scanner!!.startScan(null,ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0).build(),object:ScanCallback(){
            override fun onScanResult(cb:Int,r:ScanResult){val ad=r.device.address;val nm=r.device.name?:r.scanRecord?.deviceName?:"Unknown"
                val svcs=r.scanRecord?.serviceUuids?.map{it.uuid.toString().take(8)}?:listOf()
                val now=System.currentTimeMillis();val old=devices[ad]
                devices[ad]=BleDev(ad,nm,r.rssi,old?.firstSeen?:now,now,(old?.count?:0)+1,svcs);scanCnt++
                if(favs.contains(ad)&&old==null)runOnUiThread{Toast.makeText(this@MainActivity,"\u2B50 ${nm} appeared!",Toast.LENGTH_SHORT).show()}
                runOnUiThread{countPill.text="${devices.size}";statusLine.text="\u25CF ${devices.size} devices \u2022 $scanCnt scans"
                    if(tab in listOf(0,4))show();if(tab==1&&targetAddr==ad)show()}}
            override fun onScanFailed(c:Int){statusLine.text="\u26A0 Scan error"}})}catch(e:Exception){statusLine.text="\u26A0 ${e.message?.take(40)}"}}

    fun getOUI(addr:String):String{val p=addr.replace(":","").uppercase().take(6)
        return when(p.take(3)){"00E","08D","04C","A47","B83","C0D","CC0","D4D","E0B","F09"->"Apple";"00E","085","043","A45","B81","C04","CC0","D4E","E00","F00"->"Samsung"
        "04E","086","0C6","A4F","B8F","C4D","CC9","D4F","E8F","F8F"->"Xiaomi";"00C","084","04F","A48","B86","C4C","CC1","D4C","E4F"->"OnePlus"
        "04F","088","08F","A4C","B84","C47","CC8","D47","E88","F88"->"Google";"00A","04D","08A","A44","B8E","C44","CCD","D44","E48","F48"->"Huawei"
        else->"Unknown"}}

    fun peers()=devices.values.distinctBy{it.addr}.map{BleDev(it.addr,it.name,it.rssi,0,0,0,emptyList())}.take(20)
    fun exportScan(){val sb=StringBuilder();sb.appendLine("BLE Locator Report\n${Date()}\nGPS: $lat, $lng\n")
        devices.values.sortedByDescending{it.rssi}.forEach{d->val dist=when{d.rssi>-40->"<1m";d.rssi>-55->"3m";d.rssi>-70->"10m";else->"50m+"}
            sb.appendLine("${d.name},${d.rssi}dBm,$dist,${d.count}x,${if(favs.contains(d.addr))"FAV" else "-"}")}
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,sb.toString())},"Share Report"))}

    // UI Helpers
    fun card(parent:LinearLayout,content:View){val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,16,20,16);setBackgroundDrawable(cardBg());layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,6,16,6)}};c.addView(content);parent.addView(c)}
    fun tv(t:String,c:Int,s:Float,bold:Boolean)=TextView(this).apply{text=t;setTextColor(c);textSize=s;if(bold)setTypeface(null,Typeface.BOLD)}
    fun cardBg()=GradientDrawable().apply{shape=GradientDrawable.RECTANGLE;cornerRadius=dp(14).toFloat();setColor(0xFF181820.toInt());setStroke(1,0xFF22222a.toInt())}
    fun pill(c:Int)=GradientDrawable().apply{shape=GradientDrawable.RECTANGLE;cornerRadius=dp(8).toFloat();setColor(c);setStroke(1,c and 0x20ffffff.toInt())}
    fun empty(m:String){tv(m,gr,13f,false).apply{gravity=Gravity.CENTER;setPadding(0,40,0,40)}.also{feed.addView(it)}}
    fun dp(d:Int):Int=(d*resources.displayMetrics.density).toInt()
    fun saveFavs(){getSharedPreferences("ble",0).edit().putStringSet("favs",favs).apply()}
    fun loadFavs(){getSharedPreferences("ble",0).getStringSet("favs",null)?.let{favs.addAll(it)}}
    fun startLoc(){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return;(getSystemService(Context.LOCATION_SERVICE) as LocationManager).requestLocationUpdates(LocationManager.GPS_PROVIDER,3000,2f){lat=it.latitude;lng=it.longitude}}
    override fun onRequestPermissionsResult(rc:Int,p:Array<String>,g:IntArray){super.onRequestPermissionsResult(rc,p,g);if(g.all{it==PackageManager.PERMISSION_GRANTED})recreate()}
    override fun onDestroy(){super.onDestroy();gattServer?.close();gatt?.disconnect();gatt?.close();try{scanner?.stopScan(object:ScanCallback(){})}catch(e:Exception){}}
}
