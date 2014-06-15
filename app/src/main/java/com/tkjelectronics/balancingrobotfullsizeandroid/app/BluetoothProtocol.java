/*************************************************************************************
 * Copyright (C) 2012-2014 Kristian Lauszus, TKJ Electronics. All rights reserved.
 *
 * This software may be distributed and modified under the terms of the GNU
 * General Public License version 2 (GPL2) as published by the Free Software
 * Foundation and appearing in the file GPL2.TXT included in the packaging of
 * this file. Please note that GPL2 Section 2[b] requires that all works based
 * on this software must also be made publicly available under the terms of
 * the GPL2 ("Copyleft").
 *
 * Contact information
 * -------------------
 *
 * Kristian Lauszus, TKJ Electronics
 * Web      :  http://www.tkjelectronics.com
 * e-mail   :  kristianl@tkjelectronics.com
 *
 ************************************************************************************/


package com.tkjelectronics.balancingrobotfullsizeandroid.app;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothProtocol {
    // Debugging
    private static final String TAG = "BluetoothProtocol";
    private static final boolean D = BalancingRobotFullSizeActivity.D;

    static final byte SET_PID = 0;
    static final byte GET_PID = 1;
    static final byte SET_TARGET = 2;
    static final byte GET_TARGET = 3;
    static final byte SET_TURNING = 4;
    static final byte GET_TURNING = 5;
    static final byte SET_KALMAN = 6;
    static final byte GET_KALMAN = 7;

    static final String commandHeader = "$S>"; // Standard command header
    static final String responseHeader = "$S<"; // Standard response header

    BluetoothChatService mChatService;
    private final Handler mHandler;

    public BluetoothProtocol(BluetoothChatService mChatService, Handler handler) {
        this.mChatService = mChatService;
        this.mHandler = handler;
    }


    private void sendCommand(byte output[]) {
        mChatService.write(commandHeader);
        mChatService.write(output);
        mChatService.write(getChecksum(output));
    }
/*
    // TODO: Only send the values that has actually changed
    void setAllValues() {
        setPID(); // Set PID values
        delay(10);
        setTarget(); // Set target angle
        delay(10);
        setTurning(); // Set turning scale value
        delay(10);
        setKalman(); // Set Kalman values
        delay(10);
    }

    // TODO: Only get the values that has actually changed
    void getAllValues() {
        getPID(); // Get PID values
        delay(10);
        getTarget(); // Get target angle
        delay(10);
        getTurning(); // Get turning scale value
        delay(10);
        getKalman(); // Get Kalman values
        delay(10);
    }
*/

    /**
     * Set PID values. All floats/doubles are multiplied by 100.0 before sending.
     * @param Kp    Kp value.
     * @param Ki    Ki value.
     * @param Kd    Kd value.
     */
    public void setPID(int Kp, int Ki, int Kd) {
        if (D)
            Log.i(TAG, "setPID " + Kp + " " + Ki + " " + Kd);

        byte output[] = {
                SET_PID, // Cmd
                6, // Length
                (byte)(Kp & 0xFF),
                (byte)(Kp >> 8),
                (byte)(Ki & 0xFF),
                (byte)(Ki >> 8),
                (byte)(Kd & 0xFF),
                (byte)(Kd >> 8),
        };
        sendCommand(output); // Set PID values
    }

    /** Use this to request PID values. */
    public void getPID() {
        byte output[] = {
                GET_PID, // Cmd
                0, // Length
        };
        sendCommand(output); // Send output
    }

    /**
     * Set the target angle of the robot. All floats/doubles are multiplied by 100.0 before sending.
     * @param targetAngle   Target angle value.
     */
    public void setTarget(int targetAngle) {
        if (D)
            Log.i(TAG, "setTarget: " + targetAngle);

        byte output[] = {
                SET_TARGET, // Cmd
                2, // Length
                (byte)(targetAngle & 0xFF),
                (byte)(targetAngle >> 8),
        };
        sendCommand(output); // Set PID values
    }

    public void getTarget() {
        byte output[] = {
                GET_TARGET, // Cmd
                0, // Length
        };
        sendCommand(output); // Send output
    }

    public void setTurning(byte turningValue) {
        byte output[] = {
                SET_TURNING, // Cmd
                1, // Length
                turningValue,
        };
        sendCommand(output); // Set PID values
    }

    public void getTurning() {
        byte output[] = {
                GET_TURNING, // Cmd
                0, // Length
        };
        sendCommand(output); // Send output
    }

    /**
     * Set the Kalman coeficients. All floats/doubles are multiplied by 10000.0 before sending.
     * See: http://blog.tkjelectronics.dk/2012/09/a-practical-approach-to-kalman-filter-and-how-to-implement-it/.
     * @param Qangle    Qangle
     * @param Qbias     Qbias
     * @param Rmeasure  Rmeasure
     */

    public void setKalman(int Qangle, int Qbias, int Rmeasure) {
        byte output[] = {
                SET_KALMAN, // Cmd
                6, // Length
                (byte)(Qangle & 0xFF),
                (byte)(Qangle >> 8),
                (byte)(Qbias & 0xFF),
                (byte)(Qbias >> 8),
                (byte)(Rmeasure & 0xFF),
                (byte)(Rmeasure >> 8),
        };
        sendCommand(output); // Set PID values
    }

    public void getKalman() {
        byte output[] = {
                GET_KALMAN, // Cmd
                0, // Length
        };
        sendCommand(output); // Send output
    }

    private byte[] concat(byte[] A, byte[] B) { // Source: http://stackoverflow.com/a/80503/2175837
        int aLen = A.length;
        int bLen = B.length;
        byte[] C = new byte[aLen + bLen];
        System.arraycopy(A, 0, C, 0, aLen);
        System.arraycopy(B, 0, C, aLen, bLen);
        return C;
    }


    private byte[] buffer = new byte[1024];

    public void parseData(byte msg[], int offset, int length) {
        System.arraycopy(msg, offset, buffer, 0, length);

        int[] data = new int[length];
        for (int i = 0; i < length; i++) {
            data[i] = buffer[i] & 0xFF; // Cast to unsigned value
            if (D)
                Log.d(TAG, "Data[" + i + "]: " + data[i]);
        }

        String readMessage = new String(buffer, 0, length);

        if (D)
            Log.d(TAG, "Received string: " + readMessage);

        if (length < responseHeader.length() + 2) // We should have at least received the header, cmd and length
            return;

        // TODO: Remove whitespace in front
        if (new String(buffer).startsWith(responseHeader)) {
            int cmd = data[responseHeader.length()];
            int msgLength = data[responseHeader.length() + 1];
            if (msgLength > (length - responseHeader.length() - 3)) // Check if there is enough data - there needs to be the header, cmd, length, data and checksum
                return;

            int input[] = new int[msgLength];
            int i;
            for (i = 0; i < msgLength; i++)
                input[i] = data[i + responseHeader.length() + 2];
            int checksum = data[i + responseHeader.length() + 2];

            if (checksum == (cmd ^ msgLength ^ getChecksum(input))) {
                Message message;
                Bundle bundle;
                switch (cmd) {
                    case GET_PID:
                        int Kp = input[0] | (input[1] << 8);
                        int Ki = input[2] | (input[3] << 8);
                        int Kd = input[4] | (input[5] << 8);

                        // TODO: Just store this as an int
                        message = mHandler.obtainMessage(BalancingRobotFullSizeActivity.MESSAGE_READ); // Send message back to the UI Activity
                        bundle = new Bundle();
                        bundle.putString(BalancingRobotFullSizeActivity.KP_VALUE, String.format("%.2f", (float)Kp / 100.0f));
                        bundle.putString(BalancingRobotFullSizeActivity.KI_VALUE, String.format("%.2f", (float)Ki / 100.0f));
                        bundle.putString(BalancingRobotFullSizeActivity.KD_VALUE, String.format("%.2f", (float)Kd / 100.0f));
                        message.setData(bundle);
                        mHandler.sendMessage(message);

                        if (D)
                            Log.i(TAG, "PID: " + Kp + " " + Ki + " " + Kd);
                        break;
                    case GET_TARGET:
                        int target = input[0] | ((byte) input[1] << 8); // This can be negative as well

                        // TODO: Just store this as an int
                        message = mHandler.obtainMessage(BalancingRobotFullSizeActivity.MESSAGE_READ); // Send message back to the UI Activity
                        bundle = new Bundle();
                        bundle.putString(BalancingRobotFullSizeActivity.TARGET_ANGLE, String.format("%.2f", (float)target / 100.0f));
                        message.setData(bundle);
                        mHandler.sendMessage(message);

                        if (D)
                            Log.i(TAG, "Target: " + Integer.toString(target));
                        break;

                    // TODO: Implement the rest of the protocol
                    case GET_TURNING:
                        //turnScale.setText(Integer.toString(input[0]));
                        if (D)
                            Log.i(TAG, "Turning: " + Integer.toString(input[0]));
                        break;
                    case GET_KALMAN:
                        int QangleValue = input[0] | (input[1] << 8);
                        int QbiasValue = input[2] | (input[3] << 8);
                        int RmeasureValue = input[4] | (input[5] << 8);

                        /*Qangle.setText(Float.toString((float) QangleValue / 10000.0));
                        Qbias.setText(Float.toString((float) QbiasValue / 10000.0));
                        Rmeasure.setText(Float.toString((float) RmeasureValue / 10000.0));*/

                        if (D)
                            Log.i(TAG, "Kalman: " + QangleValue + " " + QbiasValue + " " + RmeasureValue);
                        break;
                    default:
                        if (D)
                            Log.e(TAG, "Unknown command");
                        break;
                }
            } else {
                if (D)
                    Log.e(TAG, "Checksum error!");
            }
        } else {
            if (D)
                Log.e(TAG, "Wrong header! " + readMessage);
        }
    }

    // TODO: Combine these two
    private byte getChecksum(byte data[]) {
        byte checksum = 0;
        for (byte val : data)
            checksum ^= val;
        return checksum;
    }

    private int getChecksum(int data[]) {
        int checksum = 0;
        for (int val : data)
            checksum ^= val;
        return checksum;
    }
}