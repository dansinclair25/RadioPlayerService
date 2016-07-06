package co.mobiwise.library.radio;

import android.media.AudioTrack;

/**
 * Created by dansinclair on 06/07/16.
 */
public interface RadioPlayerServiceListener {

    void onAudioTrackCreated(AudioTrack audioTrack);
}
