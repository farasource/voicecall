package ghasemi.abbas.voicecall;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;

public class SoundPlay extends Thread {

    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private final int BUFFER_SIZE = Math.max(AudioRecord.getMinBufferSize(SoundRecord.SAMPLING_RATE_IN_HZ,
            AudioFormat.CHANNEL_IN_MONO, AUDIO_FORMAT), SoundRecord.SAMPLING_RATE_IN_HZ);

    private AudioTrack audioTrack;
    private final boolean volumeUp;

    public SoundPlay(boolean volumeUp) {
        this.volumeUp = volumeUp;
    }

    public void write(byte[] bytes, int len) {
        if (audioTrack == null || audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
            return;
        }
        try {
            audioTrack.write(bytes, 0, len);
        } catch (Exception e) {
            //
        }
    }

    public void release() {
        if (audioTrack == null) {
            return;
        }
        try {
            audioTrack.stop();
            audioTrack.release();
        } catch (Exception e) {
            //
        }
        audioTrack = null;
    }

    @Override
    public void run() {
        super.run();
        audioTrack = new AudioTrack(volumeUp ? AudioManager.STREAM_MUSIC : AudioManager.STREAM_VOICE_CALL, SoundRecord.SAMPLING_RATE_IN_HZ,
                CHANNEL_CONFIG, AUDIO_FORMAT,
                BUFFER_SIZE, AudioTrack.MODE_STREAM);
        audioTrack.play();
    }

    public int getBufferSize() {
        return BUFFER_SIZE;
    }
}