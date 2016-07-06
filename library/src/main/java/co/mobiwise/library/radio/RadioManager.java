package co.mobiwise.library.radio;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by mertsimsek on 03/07/15.
 */
public class RadioManager implements IRadioManager, RadioPlayerServiceListener {

    /**
     * Logging enable/disable
     */
    private static boolean isLogging = false;

    /**
     * Singleton
     */
    private static RadioManager instance = null;

    /**
     * RadioPlayerService
     */
    private static RadioPlayerService mService;

    /**
     * Context
     */
    private Context mContext;

    /**
     * Listeners
     */
    private List<RadioListener> mRadioListenerQueue;

    /**
     * Service connected/Disconnected lock
     */
    private boolean isServiceConnected;

    private boolean enableNotifications = false;

    private boolean shouldFade = false;

    private float fadeDuration = (float)1.0;
    private float volumeIncrement = (float)0.1;

    private float mVolume = (float)0.0;

    private AudioTrack mAudioTrack;

    private Handler fadeHandler;

    private String mStreamURL;

    /**
     * Private constructor because of Singleton pattern
     * @param mContext
     */
    private RadioManager(Context mContext) {
        this.mContext = mContext;
        mRadioListenerQueue = new ArrayList<>();
        isServiceConnected = false;
        fadeHandler = new Handler();

    }

    /**
     * Singleton
     * @param mContext
     * @return
     */
    public static RadioManager with(Context mContext) {
        if (instance == null)
            instance = new RadioManager(mContext);
        return instance;
    }

    /**
     * get current service instance
     * @return RadioPlayerService
     */
    public static RadioPlayerService getService(){
        return mService;
    }

    public void setEnableNotifications(boolean enable) {
        enableNotifications = enable;
    }

    public void setShouldFade(boolean fade) {
        shouldFade = fade;
    }

    public void setVolume(float volume) {
        if (Build.VERSION.SDK_INT >= 21) {
            mVolume = volume;
            mService.mAudioTrack.setVolume(mVolume);
        }
    }

    public float getVolume() {
        if (Build.VERSION.SDK_INT >= 21) {
            return mVolume;
        }
        return (float)1.0;
    }

    Runnable fadeInRunnable = new Runnable() {
        @Override
        public void run() {
            if (mVolume < (float)1.0) {
                setVolume(mVolume + volumeIncrement);
                long delay = (long) (fadeDuration * volumeIncrement);
                fadeHandler.postDelayed(this, delay);
            } else {
                mService.play(mStreamURL);
            }
        }
    };

    Runnable fadeOutRunnable = new Runnable() {
        @Override
        public void run() {
            if (mVolume >= (float)0.0) {
                setVolume(mVolume - volumeIncrement);
                long delay = (long) (fadeDuration * volumeIncrement);
                fadeHandler.postDelayed(this, delay);
            } else {
                mService.stop();
            }
        }
    };

    @Override
    public void onAudioTrackCreated(AudioTrack audioTrack) {
        mAudioTrack = audioTrack;
        log("onAudioTrackCreated: " + mAudioTrack);
        if (shouldFade && Build.VERSION.SDK_INT >= 21 && mAudioTrack != null && mService.isPlaying()) {
            mAudioTrack.setVolume((float)0.0);
            fadeInRunnable.run();
        }
    }

    /**
     * Start Radio Streaming
     * @param streamURL
     */
    @Override
    public void startRadio(String streamURL) {
        mStreamURL = streamURL;

        mService.play(mStreamURL);

    }


    /**
     * Stop Radio Streaming
     */
    @Override
    public void stopRadio() {
        if (shouldFade && Build.VERSION.SDK_INT >= 21) {

            fadeOutRunnable.run();

        } else {
            mService.stop();
        }
    }

    /**
     * Check if radio is playing
     * @return
     */
    @Override
    public boolean isPlaying() {
        log("IsPlaying : " + mService.isPlaying());
        return mService.isPlaying();
    }

    /**
     * Register listener to listen radio service actions
     * @param mRadioListener
     */
    @Override
    public void registerListener(RadioListener mRadioListener) {
        if (isServiceConnected)
            mService.registerListener(mRadioListener);
        else
            mRadioListenerQueue.add(mRadioListener);
    }

    /**
     * Unregister listeners
     * @param mRadioListener
     */
    @Override
    public void unregisterListener(RadioListener mRadioListener) {
        log("Register unregistered.");
        mService.unregisterListener(mRadioListener);
    }

    /**
     * Set/Unset Logging
     * @param logging
     */
    @Override
    public void setLogging(boolean logging) {
        isLogging = logging;
    }

    /**
     * Connect radio player service
     */
    @Override
    public void connect() {
        log("Requested to connect service.");
        Intent intent = new Intent(mContext, RadioPlayerService.class);
        mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void destroy() {
        log("destroy");
        RadioManager.getService().cancelNotifications();
        RadioManager.getService().destroy();
        disconnect();
    }

    /**
     * Disconnect radio player service
     */
    @Override
    public void disconnect() {
        log("Service Disconnected.");
        mContext.unbindService(mServiceConnection);
    }

    /**
     * Update notification data
     * @param singerName
     * @param songName
     * @param smallArt
     * @param bigArt
     */
    @Override
    public void updateNotification(String singerName, String songName, int smallArt, int bigArt) {
        if(mService != null)
            mService.updateNotification(singerName, songName, smallArt, bigArt);
    }

    /**
     * Update notification data
     * @param singerName
     * @param songName
     * @param smallArt
     * @param bigArt
     */
    @Override
    public void updateNotification(String singerName, String songName, int smallArt, Bitmap bigArt) {
        if(mService != null)
            mService.updateNotification(singerName, songName, smallArt, bigArt);
    }

    /**
     * Connection
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder) {

            log("Service Connected.");

            mService = ((RadioPlayerService.LocalBinder) binder).getService();
            mService.setLogging(isLogging);
            isServiceConnected = true;
            mService.setEnableNotifications(enableNotifications);

            if (!mRadioListenerQueue.isEmpty()) {
                for (RadioListener mRadioListener : mRadioListenerQueue) {
                    registerListener(mRadioListener);
                    mRadioListener.onRadioConnected();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    /**
     * Logger
     * @param log
     */
    private void log(String log) {
        if (isLogging)
            Log.v("RadioManager", "RadioManagerLog : " + log);
    }

}
