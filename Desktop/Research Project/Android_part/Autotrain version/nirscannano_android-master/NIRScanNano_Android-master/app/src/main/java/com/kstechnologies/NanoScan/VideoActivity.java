package com.kstechnologies.NanoScan;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.util.Log;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;
import java.io.File;
import android.media.MediaPlayer.OnPreparedListener;


/**
 * Created by frankie on 2018/8/11.
 */

public class VideoActivity extends Activity {

    private VideoView videoview;
    private int position = 0;

    //Tag for logging events in the BluetoothLeService
    private static final String TAG = "__NewScanActivity";
    /** Called when the activity is firstcreated. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        Log.d(TAG, "begin video activity");
        videoview = (VideoView) findViewById(R.id.video);
        File file = new File("/storage/emulated/0/2.mp4");
        MediaController mc = new MediaController(VideoActivity.this);       // 创建一个MediaController对象
        if(file.exists()){
            videoview.setVideoPath(file.getAbsolutePath());
            videoview.setMediaController(mc);       // 将VideoView与MediaController关联起来
            videoview.requestFocus();       // 设置VideoView获取焦点
            /*try {
                //videoview.start();      // 播放视频
                if (position == 0) {
                    videoview.start();
                } else {
                    //if we come from a resumed activity, video playback will be paused
                    videoview.pause();
                }
            }catch(Exception e) {
                e.printStackTrace();
            }*/
            //we also set an setOnPreparedListener in order to know when the video file is ready for playback
            videoview.setOnPreparedListener(new OnPreparedListener() {

                public void onPrepared(MediaPlayer mediaPlayer) {
                    //if we have a position on savedInstanceState, the video playback should start from here
                    videoview.seekTo(position);
                    if (position == 0) {
                        videoview.start();
                    } else {
                        //if we come from a resumed activity, video playback will be paused
                        videoview.pause();
                    }
                }
            });

            // 设置VideoView的Completion事件监听器
            videoview.setOnCompletionListener(new OnCompletionListener(){
                @Override
                public void onCompletion (MediaPlayer mp) {
                    Toast.makeText(VideoActivity.this, "The video has finished playing！", Toast.LENGTH_SHORT).show();
                }
            });
        }else{
            Toast.makeText(this, "The video file to play does not exist!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("Position", videoview.getCurrentPosition());
        videoview.pause();
    }
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        position = savedInstanceState.getInt("Position");
        videoview.seekTo(position);
    }

}
