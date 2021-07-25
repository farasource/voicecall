package ghasemi.abbas.voicecall;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;


import java.io.ByteArrayOutputStream;

import java.util.ArrayList;

public class VoiceCallActivity extends AppCompatActivity {

    private final int REQUEST_PERMISSION_CODE = 1001;
    public boolean volumeUp = false;
    private boolean sending;

    private FrameLayout mRootContent;
    private FrameLayout voiceCallBtn;
    private TextView message;
    private TextView timer;
    private AppCompatImageView volume;
    private AppCompatImageView voiceCallImg;

    private final ArrayList<Bundle> queue = new ArrayList<>();
    private SoundRecord soundRecord;
    private VoiceCallGET voiceCall;
    private NetworkChangeReceiver receiver;
    private Handler handler;
    private long time = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_voicecall);

        mRootContent = findViewById(R.id.root);
        message = findViewById(R.id.message);
        timer = findViewById(R.id.timer);
        volume = findViewById(R.id.volumeImg);
        voiceCallBtn = findViewById(R.id.voiceCall);
        voiceCallImg = findViewById(R.id.voiceCallImg);

        receiver = new NetworkChangeReceiver(() -> {
            if (CheckNetworkState.isOnline()) {
                if (voiceCall == null) {
                    voiceCall = new VoiceCallGET();
                    voiceCall.start();
                }
            } else {
                if (voiceCall != null) {
                    voiceCall.destroyConnection();
                }
            }
        });
        findViewById(R.id.volume).setOnClickListener(v -> {
            volumeUp = !volumeUp;
            if (volumeUp) {
                volume.setImageResource(R.drawable.ic_baseline_volume_down_24);
            } else {
                volume.setImageResource(R.drawable.ic_baseline_volume_up_24);
            }
            if (voiceCall != null) {
                voiceCall.restartSoundPlayer();
            }
        });
        findViewById(R.id.end).setOnClickListener(v -> finish());
        voiceCallBtn.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSION_CODE);
                return;
            }
            if (soundRecord == null) {
                voiceCallBtn.setBackgroundResource(R.drawable.voice_call_on);
                voiceCallImg.setImageResource(R.drawable.ic_baseline_mic_24);
                voiceCallImg.animate()
                        .setDuration(1500)
                        .scaleY(1.4f)
                        .scaleX(1.4f)
                        .start();
                soundRecord = new SoundRecord();
                soundRecord.setReader(this::addQueue);
                soundRecord.start();
            } else {
                voiceCallBtn.setBackgroundResource(R.drawable.voice_call_off);
                voiceCallImg.setImageResource(R.drawable.ic_baseline_mic_off_24);
                voiceCallImg.animate()
                        .setDuration(1500)
                        .scaleY(1f)
                        .scaleX(1f)
                        .start();
                soundRecord.destroyRecorder();
                soundRecord = null;
            }
        });
        voiceCall = new VoiceCallGET();
        voiceCall.start();

        handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                time++;
                int m = (int) (time / 60);
                int s = (int) (time - m * 60);
                timer.setText(String.format("%02d:%02d", m, s));
                handler.postDelayed(this, 1000);
            }
        });

        AnimationDrawable frameAnimation = (AnimationDrawable) mRootContent.getBackground();
        frameAnimation.setEnterFadeDuration(3500);
        frameAnimation.setExitFadeDuration(3500);
        frameAnimation.start();
    }

    private void addQueue(byte[] buffer, int len) {
        Bundle bundle = new Bundle();
        bundle.putByteArray("buffer", buffer);
        bundle.putInt("len", len);
        queue.add(bundle);
        request();
    }

    private void request() {
        if (!sending) {
            sending = true;
            new Thread(this::VoiceCallPOST).start();
        }
    }

    private void VoiceCallPOST() {
        synchronized (this) {
            if (queue.isEmpty()) {
                sending = false;
                return;
            }
        }
        //////////////
        /////POST/////
        //////////////
        queue.remove(0);
        VoiceCallPOST();
    }

    private class VoiceCallGET extends Thread {

        private boolean allowReceive = true;
        private SoundPlay soundPlay = new SoundPlay(volumeUp);
        private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        @Override
        public void run() {
            super.run();
            soundPlay.start();
            request();
        }

        private void request() {
            if (!allowReceive) {
                return;
            }
            runOnUiThread(() -> {
                message.setBackgroundResource(R.drawable.connection_wait);
                message.setText("در حال برقرای ارتباط ...");
            });

            /////////////
            /////GET/////
            /////////////
//            runOnUiThread(() -> {
//                message.setBackgroundResource(R.drawable.connection_ok);
//                message.setText("ارتباط برقرار است.");
//            });

            destroyConnection();
            runOnUiThread(() -> {
                message.setBackgroundResource(R.drawable.connection_none);
                message.setText("دسترسی به شبکه وجود ندارد.");
            });
        }

        public void restartSoundPlayer() {
            SoundPlay sp = new SoundPlay(volumeUp);
            sp.start();
            soundPlay.release();
            soundPlay = sp;
        }

        public void destroyConnection() {
            allowReceive = false;
            if (soundPlay != null) {
                soundPlay.release();
                soundPlay = null;
            }
            voiceCall = null;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        TextView textView = findViewById(R.id.statusBar);
        textView.setHeight(getStatusBarHeight());
    }

    @Override
    protected void onResume() {
        super.onResume();
//        mRootContent.setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_LOW_PROFILE
//        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundRecord != null) {
            soundRecord.destroyRecorder();
            soundRecord = null;
        }
        if (voiceCall != null) {
            voiceCall.destroyConnection();
            voiceCall = null;
        }
        if (receiver != null) {
            receiver.destroy();
            receiver = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                voiceCallBtn.performClick();
            } else {
                Toast.makeText(this, "برای ایجاد تماس دسترسی به میکروفون مورد نیاز است.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public int getStatusBarHeight() {
        final Resources resources = getResources();
        final int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId <= 0) {
            return (int) Math.ceil((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 24 : 25) * resources.getDisplayMetrics().density);
        } else {
            return resources.getDimensionPixelSize(resourceId);
        }
    }
}