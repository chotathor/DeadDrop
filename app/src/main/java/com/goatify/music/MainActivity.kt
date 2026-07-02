package com.goatify.music

import android.content.*; import android.database.*; import android.graphics.*; import android.media.*
import android.net.Uri; import android.os.*; import android.provider.MediaStore; import android.view.*
import android.widget.*; import androidx.appcompat.app.AppCompatActivity; import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat; import android.Manifest; import android.content.pm.PackageManager
import java.io.File; import java.util.*

class MainActivity : AppCompatActivity() {
    val bg=0xFF0a0a10.toInt();val grn=0xFF00d684.toInt();val tx=0xFFf0f0f0.toInt();val dim=0xFF666688.toInt()
    val songs=mutableListOf<Song>();var currentIdx=-1;var mediaPlayer:MediaPlayer?=null;var isPlaying=false
    lateinit var feed:LinearLayout;lateinit var playerBar:LinearLayout;lateinit var songTitle:TextView;lateinit var playBtn:TextView;lateinit var seekBar:SeekBar;val handler=Handler(Looper.getMainLooper())
    data class Song(val title:String,val artist:String,val path:String,val duration:Long)

    override fun onCreate(s:Bundle?){super.onCreate(s)
        if(checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO)!=PackageManager.PERMISSION_GRANTED&&
           checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)
            {requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_AUDIO,Manifest.permission.READ_EXTERNAL_STORAGE),42);return}
        window.statusBarColor=bg;window.navigationBarColor=bg
        buildUI();setContentView(findViewById(android.R.id.content) ?: run{val l=LinearLayout(this);setContentView(l);l})
        loadSongs()}

    fun buildUI(){val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(0,32,0,0)}
        val hdr=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(20,16,20,12);gravity=Gravity.CENTER_VERTICAL}
        hdr.addView(tv("GOATIFY",grn,22f,true).apply{layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
        hdr.addView(tv("Music",dim,12f));root.addView(hdr)
        feed=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(0,4,0,0)};root.addView(ScrollView(this).apply{addView(feed)})
        playerBar=buildPlayer();playerBar.visibility=View.GONE;root.addView(playerBar);setContentView(root)}

    fun buildPlayer():LinearLayout{val bar=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(0xFF080810.toInt());setPadding(20,12,20,20);layoutParams=LinearLayout.LayoutParams(-1,-2).apply{gravity=Gravity.BOTTOM}}
        val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        val info=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
        songTitle=info.addView(tv("No song",tx,14f,true)) as TextView
        info.addView(tv("",dim,11f).apply{setPadding(0,2,0,0)})
        row.addView(info)
        playBtn=row.addView(tv("\u25B6",grn,28f,true)) as TextView;playBtn.setPadding(16,12,16,12);playBtn.setOnClickListener{togglePlay()}
        bar.addView(row)
        seekBar=SeekBar(this).apply{layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,8,0,0)};setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(s:SeekBar,p:Int,from:Boolean){if(from)mediaPlayer?.seekTo(p*1000)}
            override fun onStartTrackingTouch(p:SeekBar?){};override fun onStopTrackingTouch(p:SeekBar?){}})}
        bar.addView(seekBar);return bar}

    fun loadSongs(){val songs=querySongs();if(songs.isNotEmpty()){this.songs.addAll(songs);showSongs()}else feed.addView(tv("No songs found on device",dim,14f).apply{gravity=Gravity.CENTER;setPadding(0,40,0,40)})}

    fun querySongs():List<Song>{val list=mutableListOf<Song>()
        try{val uri=MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;val proj=arrayOf(MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.DATA,MediaStore.Audio.Media.DURATION)
            val cur=contentResolver.query(uri,proj,null,null,"${MediaStore.Audio.Media.TITLE} ASC");cur?.use{
                val ti=it.getColumnIndex(MediaStore.Audio.Media.TITLE);val ai=it.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val di=it.getColumnIndex(MediaStore.Audio.Media.DATA);val dui=it.getColumnIndex(MediaStore.Audio.Media.DURATION)
                while(it.moveToNext()){val t=it.getString(ti)?:continue;val a=it.getString(ai)?:"Unknown";val d=it.getString(di)?:continue
                    val dur=it.getLong(dui);list.add(Song(t,a,d,dur))}}}catch(e:Exception){feed.addView(tv("Permission needed for music",dim,13f).apply{gravity=Gravity.CENTER;setPadding(0,20,0,20)})};return list}

    fun showSongs(){feed.removeAllViews()
        songs.forEachIndexed{i,s->val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(20,14,20,14);layoutParams=LinearLayout.LayoutParams(-1,-2);setBackgroundColor(0xFF0a0a14.toInt());(layoutParams as LinearLayout.LayoutParams).setMargins(12,2,12,2)}
            val left=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
            left.addView(tv(s.title,if(i==currentIdx)grn else tx,14f,true))
            left.addView(tv("${s.artist}  \u2022  ${formatDur(s.duration)}",dim,11f).apply{setPadding(0,2,0,0)})
            row.addView(left);row.setOnClickListener{play(i)};feed.addView(row)}}

    fun play(idx:Int){if(songs.isEmpty())return;currentIdx=idx;stopPlayer()
        mediaPlayer=MediaPlayer().apply{try{setDataSource(songs[idx].path);prepare();start();isPlaying=true}
            catch(e:Exception){toast("Cannot play: ${songs[idx].title}")}}
        showSongs();playerBar.visibility=View.VISIBLE;songTitle.text=songs[idx].title
        seekBar.max=(songs[idx].duration/1000).toInt();playBtn.text="\u23F8";startSeekUpdate()}

    fun togglePlay(){if(mediaPlayer==null)return
        if(isPlaying){mediaPlayer?.pause();playBtn.text="\u25B6";isPlaying=false}
        else{mediaPlayer?.start();playBtn.text="\u23F8";isPlaying=true}}

    fun stopPlayer(){isPlaying=false;mediaPlayer?.stop();mediaPlayer?.release();mediaPlayer=null}

    fun startSeekUpdate(){handler.postDelayed(object:Runnable{override fun run(){
            try{seekBar.progress=(mediaPlayer?.currentPosition?:0)/1000;handler.postDelayed(this,1000)}catch(e:Exception){}}},1000)}

    fun formatDur(ms:Long):String{val m=ms/60000;val s=(ms%60000)/1000;return "${m}:${s.toString().padStart(2,'0')}"}

    fun tv(t:String,c:Int,s:Float,bold:Boolean=false)=TextView(this).apply{text=t;setTextColor(c);textSize=s;if(bold)setTypeface(null,Typeface.BOLD)}
    fun toast(m:String){Toast.makeText(this,m,Toast.LENGTH_SHORT).show()}
    override fun onRequestPermissionsResult(rc:Int,p:Array<String>,g:IntArray){super.onRequestPermissionsResult(rc,p,g);if(g.all{it==PackageManager.PERMISSION_GRANTED})recreate()}
    override fun onDestroy(){super.onDestroy();stopPlayer()}
}
