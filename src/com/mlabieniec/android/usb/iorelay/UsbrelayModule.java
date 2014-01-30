/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package com.mlabieniec.android.usb.iorelay;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;

import org.appcelerator.titanium.TiApplication;
import org.appcelerator.kroll.common.Log;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

@Kroll.module(name="Usbrelay", id="com.mlabieniec.android.usb.iorelay")
public class UsbrelayModule extends KrollModule
{

	private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private D2xxManager ftdid2xx;
	private FT_Device ftDev = null;
    private UsbDeviceConnection mConnection;
    private UsbEndpoint ep;
    private UsbEndpoint ctrl;
    private PendingIntent mPermissionIntent;

	// Standard Debugging variables
	private static final String TAG = "USB";
	
	// this should reflect the name of your titanium app package
	private static final String ACTION_USB_PERMISSION =
    	    "com.mlabieniec.iotester.USB_PERMISSION";
    	
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        if (ACTION_USB_PERMISSION.equals(action)) {
	            synchronized (this) {
	                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

	                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
	                    if(device != null){
	                      setDevice(device);
	                   }
	                } 
	                else {
	                    Log.d(TAG, "permission denied for device " + device);
	                }
	            }
	        }
	    }
	    
	};
	
	// You can define constants with @Kroll.constant, for example:
	// @Kroll.constant public static final String EXTERNAL_NAME = value;
	public UsbrelayModule()
	{
		super();
	}

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app)
	{
		Log.d(TAG, "inside onAppCreate");
	}
	
	@Kroll.method
	public Boolean initUsb()
	{
		Boolean deviceFound = false;
		Activity activity = getActivity();
		Intent intent = activity.getIntent();
		
		mUsbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
		
		try {
			ftdid2xx = D2xxManager.getInstance(activity.getApplicationContext());
			Log.d(TAG,"D2xxManager=" + ftdid2xx.toString());
    	} catch (D2xxManager.D2xxException ex) {
    		Log.d(TAG,ex.getMessage());
    		return false;
    	}
		
		// Register the broadcast receiver to ask permission for the USB device
		mPermissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		activity.registerReceiver(mUsbReceiver, filter);
		
        String action = intent.getAction();
        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        
        // set the device when attached, release when detached
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            setDevice(device);
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            if (mDevice != null && mDevice.equals(device)) {
                setDevice(null);
            }
        }
        
        // Loop through all the attached devices and set our device if it's attached
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while(deviceIterator.hasNext()){
            UsbDevice foundDevice = deviceIterator.next();
            Log.d(TAG, "USB_DEVICE: " + foundDevice);
            if (foundDevice.getVendorId() == 1027) {
            	deviceFound = true;
            	Log.d(TAG, "SUPPORTED_DEVICE: " + foundDevice);
                ftdid2xx.addUsbDevice(foundDevice);
                setDevice(foundDevice);
                break;
            }
        }
        
        return deviceFound;
	}
	
	private void setDevice(UsbDevice device) {
		
		Log.d(TAG, "SETTING_DEVICE: " + device);
		
		if (device == null || device.getInterfaceCount() != 1) {
            return;
        }
		
		if (mUsbManager.hasPermission(device)) {
			
			if (device != null) {
				ftDev = ftdid2xx.openByUsbDevice(getActivity().getApplicationContext(), device);
				Log.d(TAG,"ftDev=" + ftDev);
				return;
			}
		} else {
			Log.d(TAG, "REQUESTING_PERMISSION_FOR: " + mPermissionIntent);
			mUsbManager.requestPermission(device, mPermissionIntent);
		}
    }
	
	@Kroll.method
	public Boolean setConfig(int baud, int dataBits, int stopBits, int parity, int flowControl)
	{
		if (ftDev == null) {
			Log.e(TAG, "SetConfig: ftDev is null");
			return false;
		}
		
		if (ftDev.isOpen() == false) {
			Log.e(TAG, "SetConfig: device not open");
			return false;
		}

		// configure our port
		// reset to UART mode for 232 devices
		//ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);
		ftDev.setBaudRate(baud);

		switch (dataBits) {
		case 7:
			dataBits = D2xxManager.FT_DATA_BITS_7;
			break;
		case 8:
			dataBits = D2xxManager.FT_DATA_BITS_8;
			break;
		default:
			dataBits = D2xxManager.FT_DATA_BITS_8;
			break;
		}

		switch (stopBits) {
		case 1:
			stopBits = D2xxManager.FT_STOP_BITS_1;
			break;
		case 2:
			stopBits = D2xxManager.FT_STOP_BITS_2;
			break;
		default:
			stopBits = D2xxManager.FT_STOP_BITS_1;
			break;
		}

		switch (parity) {
		case 0:
			parity = D2xxManager.FT_PARITY_NONE;
			break;
		case 1:
			parity = D2xxManager.FT_PARITY_ODD;
			break;
		case 2:
			parity = D2xxManager.FT_PARITY_EVEN;
			break;
		case 3:
			parity = D2xxManager.FT_PARITY_MARK;
			break;
		case 4:
			parity = D2xxManager.FT_PARITY_SPACE;
			break;
		default:
			parity = D2xxManager.FT_PARITY_NONE;
			break;
		}

		ftDev.setDataCharacteristics((byte)dataBits, (byte)stopBits, (byte)parity);

		short flowCtrlSetting;
		switch (flowControl) {
		case 0:
			flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
			break;
		case 1:
			flowCtrlSetting = D2xxManager.FT_FLOW_RTS_CTS;
			break;
		case 2:
			flowCtrlSetting = D2xxManager.FT_FLOW_DTR_DSR;
			break;
		case 3:
			flowCtrlSetting = D2xxManager.FT_FLOW_XON_XOFF;
			break;
		default:
			flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
			break;
		}

		// TODO : flow ctrl: XOFF/XOM
		// TODO : flow ctrl: XOFF/XOM
		ftDev.setFlowControl(flowCtrlSetting, (byte) 0x0b, (byte) 0x0d);
		logStatus();
		return true;
	}
	
	@Kroll.method
	public int pulseRelay(int relay) {
		
		if (ftDev.isOpen() == false) {
			Log.e(TAG, "SendMessage: device not open");
			return -1;
		} else {
            Log.e(TAG, "SendMessage");
        }
		
		int result = -1;
		ftDev.setLatencyTimer((byte) 16);

		switch (relay) {
			
		case 1:
				try {
					byte[] open = {(byte)254,108,01};
					ftDev.write(open);
					synchronized (ftDev) {
						ftDev.wait(200);
					}
					byte[] close = {(byte)254,100,01};
					result = ftDev.write(close);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					Log.d(TAG,e.getMessage());
					result = -1;
				}
			break;
			
			case 2:
				try {
					byte[] open = {(byte)254,109,01};
					ftDev.write(open);
					synchronized (ftDev) {
						ftDev.wait(200);
					}
					byte[] close = {(byte)254,101,01};
					result = ftDev.write(close);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					Log.d(TAG,e.getMessage());
					result = -1;
				}
			break;
			
			case 3:
				try {
					byte[] open = {(byte)254,110,01};
					ftDev.write(open);
					synchronized (ftDev) {
						ftDev.wait(200);
					}
					byte[] close = {(byte)254,102,01};
					result = ftDev.write(close);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					Log.d(TAG,e.getMessage());
					result = -1;
				}
			break;
			
			case 4:
				try {
					byte[] open = {(byte)254,111,01};
					ftDev.write(open);
					synchronized (ftDev) {
						ftDev.wait(200);
					}
					byte[] close = {(byte)254,103,01};
					result = ftDev.write(close);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					Log.d(TAG,e.getMessage());
					result = -1;
				}
			break;
			
		}
		Log.d(TAG,"Result: " + result);
        return result;
    }
	
	@Kroll.method
	public void logStatus()
	{
		short sModem = ftDev.getModemStatus();
		Log.d(TAG,"ModemStatus : " + Integer.toHexString(sModem));
		
		short sLine = ftDev.getLineStatus();
		Log.d(TAG,"LineStatus : " + Integer.toHexString(sLine));
		
		byte ltime = ftDev.getLatencyTimer();
		Log.d(TAG,"LatencyTimer : " + ltime);		
		Log.d(TAG,"Description : " + ftDev.getDeviceInfo().description);
		
	}

	// Methods
	@Kroll.method
	public String example()
	{
		Log.d(TAG, "example called");
		return "hello world";
	}
	
	// Properties
	@Kroll.getProperty
	public String getExampleProp()
	{
		Log.d(TAG, "get example property");
		return "hello world";
	}
	
	
	@Kroll.setProperty
	public void setExampleProp(String value) {
		Log.d(TAG, "set example property: " + value);
	}

}

