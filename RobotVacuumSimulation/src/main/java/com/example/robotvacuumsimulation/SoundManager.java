package com.example.robotvacuumsimulation;

import javax.sound.sampled.*;

public class SoundManager {

    private static final float SAMPLE_RATE = 22050f;

    private Clip    vacuumClip;
    private Clip    cleaningClip;
    private Clip    chargingClip;
    private boolean muted = false;

    public SoundManager() {
        try {
            vacuumClip   = buildClip(makeVacuumData());
            cleaningClip = buildClip(makeCleaningData());
            chargingClip = buildClip(makeChargingData());
        } catch (Exception e) {
            System.out.println("Ses sistemi baslatılamadı: " + e.getMessage());
        }
    }

    private byte[] makeVacuumData() {
        int    len = (int)(SAMPLE_RATE * 0.3f);
        byte[] buf = new byte[len * 2];
        for (int i = 0; i < len; i++) {
            double t = i / (double) SAMPLE_RATE;
            double v = 0.40 * Math.sin(2 * Math.PI * 80  * t)
                     + 0.25 * Math.sin(2 * Math.PI * 160 * t)
                     + 0.15 * Math.sin(2 * Math.PI * 240 * t)
                     + 0.10 * Math.sin(2 * Math.PI * 320 * t)
                     + 0.10 * (Math.random() * 2 - 1);
            short s = (short)(v * 6500);
            buf[i * 2]     = (byte)(s & 0xFF);
            buf[i * 2 + 1] = (byte)((s >> 8) & 0xFF);
        }
        return buf;
    }

    private byte[] makeCleaningData() {
        int    len = (int)(SAMPLE_RATE * 0.25f);
        byte[] buf = new byte[len * 2];
        for (int i = 0; i < len; i++) {
            double t   = i / (double) SAMPLE_RATE;
            double env = Math.exp(-t * 18);
            double v   = env * (0.55 * Math.sin(2 * Math.PI * 600 * t)
                              + 0.45 * (Math.random() * 2 - 1));
            short s = (short)(v * 9000);
            buf[i * 2]     = (byte)(s & 0xFF);
            buf[i * 2 + 1] = (byte)((s >> 8) & 0xFF);
        }
        return buf;
    }

    private byte[] makeChargingData() {
        int    len = (int)(SAMPLE_RATE * 0.7f);
        byte[] buf = new byte[len * 2];
        for (int i = 0; i < len; i++) {
            double t    = i / (double) SAMPLE_RATE;
            double freq = 300 + 200 * (t / 0.7);
            double env  = (i < len / 6)      ? (double) i / (len / 6.0)
                        : (i > len * 5 / 6)  ? (double)(len - i) / (len / 6.0)
                        : 1.0;
            double v = env * 0.5 * Math.sin(2 * Math.PI * freq * t);
            short s = (short)(v * 7000);
            buf[i * 2]     = (byte)(s & 0xFF);
            buf[i * 2 + 1] = (byte)((s >> 8) & 0xFF);
        }
        return buf;
    }

    private Clip buildClip(byte[] data) throws Exception {
        AudioFormat fmt  = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        Clip        clip = AudioSystem.getClip();
        clip.open(fmt, data, 0, data.length);
        return clip;
    }

    public void startVacuum() {
        if (muted || vacuumClip == null || vacuumClip.isRunning()) return;
        vacuumClip.setFramePosition(0);
        vacuumClip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public void stopVacuum() {
        if (vacuumClip != null && vacuumClip.isRunning()) vacuumClip.stop();
    }

    public void playCleaning() {
        if (muted || cleaningClip == null) return;
        if (cleaningClip.isRunning()) cleaningClip.stop();
        cleaningClip.setFramePosition(0);
        cleaningClip.start();
    }

    public void startCharging() {
        if (muted || chargingClip == null || chargingClip.isRunning()) return;
        chargingClip.setFramePosition(0);
        chargingClip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public void stopCharging() {
        if (chargingClip != null && chargingClip.isRunning()) chargingClip.stop();
    }

    public void stopAll() {
        stopVacuum();
        stopCharging();
        if (cleaningClip != null && cleaningClip.isRunning()) cleaningClip.stop();
    }

    public void    setMuted(boolean muted) { this.muted = muted; if (muted) stopAll(); }
    public boolean isMuted()               { return muted; }
}
