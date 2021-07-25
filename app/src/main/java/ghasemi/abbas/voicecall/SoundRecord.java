package ghasemi.abbas.voicecall;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.nio.ByteBuffer;

public class SoundRecord extends Thread {

    private AudioRecord recorder;
    private Reader reader;
    private boolean record = true;

    public final static int SAMPLING_RATE_IN_HZ = 8000; // 48000
    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private final int BUFFER_SIZE = Math.max(AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT), SAMPLING_RATE_IN_HZ);
    private int LOAD_SIZE = SAMPLING_RATE_IN_HZ * 2;

    public void setReader(Reader reader) {
        this.reader = reader;
    }

    @Override
    public void run() {
        super.run();
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLING_RATE_IN_HZ,
                CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
        loop();
    }

    private void loop() {
        while (record) {
            if (recorder.getState() == android.media.AudioRecord.STATE_INITIALIZED) {
                if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
                    recorder.startRecording();
                } else {
                    ByteBuffer bytes = ByteBuffer.allocateDirect(LOAD_SIZE);
                    int len = recorder.read(bytes, LOAD_SIZE);
                    if (len > 0 && reader != null) {
                        reader.read(bytes.array(), len);
                    }
                }
            }

        }
    }

    public interface Reader {
        void read(byte[] buffer, int len);
    }

    public void destroyRecorder() {
        if (recorder == null) {
            return;
        }
        record = false;
        try {
            recorder.stop();
            recorder.release();
        } catch (Exception e) {
            //
        }
        recorder = null;
        reader = null;
    }
}