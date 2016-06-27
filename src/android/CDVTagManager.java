/**
 * Copyright (c) 2014 Jared Dickson
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.jareddickson.cordova.tagmanager;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tagmanager.TagManager;
import com.google.android.gms.tagmanager.Container;
import com.google.android.gms.tagmanager.Container.FunctionCallMacroCallback;
import com.google.android.gms.tagmanager.Container.FunctionCallTagCallback;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.DataLayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import android.util.Log;

/**
 * This class echoes a string called from JavaScript.
 */
public class CDVTagManager extends CordovaPlugin {

    private Container mContainer;
    private boolean inited = false;

    public CDVTagManager() {
    }

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callback) {
        if (action.equals("initGTM")) {
            try {
                // Set the dispatch interval
                GoogleAnalytics.getInstance(this.cordova.getActivity()).setLocalDispatchPeriod(args.getInt(1));

                TagManager tagManager = TagManager.getInstance(this.cordova.getActivity().getApplicationContext());
                tagManager.setVerboseLoggingEnabled(false);

                String defaultBinaryContainerFileName = args.getString(0).toLowerCase().replace("-", "_");
                int defaultBinaryContainerResourceId = this.cordova.getActivity().getResources().getIdentifier(defaultBinaryContainerFileName, "raw", this.cordova.getActivity().getApplicationContext().getPackageName());

                Log.d("TagManagerPlugin", "defaultBinaryContainerResourceId: " + defaultBinaryContainerResourceId);

                PendingResult<ContainerHolder> pending = tagManager.loadContainerPreferNonDefault(args.getString(0), defaultBinaryContainerResourceId);

                // The onResult method will be called as soon as one of the following happens:
                //     1. a saved container is loaded
                //     2. if there is no saved container, a network container is loaded
                //     3. the 2-second timeout occurs
                pending.setResultCallback(new ResultCallback<ContainerHolder>() {
                    @Override
                    public void onResult(ContainerHolder containerHolder) {

                        Log.d("TagManagerPlugin", "pending container result");
                        ContainerHolderSingleton.setContainerHolder(containerHolder);

                        containerHolder.setContainerAvailableListener(new ContainerHolder.ContainerAvailableListener() {
                            @Override
                            public void onContainerAvailable(ContainerHolder containerHolder, String containerVersion) {
                                Log.d("TagManagerPlugin", "onContainerAvailable, container version: " + containerVersion);

                                mContainer = containerHolder.getContainer();
                                inited = true;

                                ContainerHolderSingleton.getContainerHolder().refresh();
                            }
                        });

                        if (containerHolder.getContainer() != null) {
                            Log.d("TagManagerPlugin", "we have a container");
                        }

                    }
                }, 2000, java.util.concurrent.TimeUnit.MILLISECONDS);

                callback.success("initGTM - id = " + args.getString(0) + "; interval = " + args.getInt(1) + " seconds");
                return true;
            } catch (final Exception e) {
                callback.error(e.getMessage());
            }
        } else if (action.equals("exitGTM")) {
            try {
                inited = false;
                callback.success("exitGTM");
                return true;
            } catch (final Exception e) {
                callback.error(e.getMessage());
            }
        } else if (action.equals("trackEvent")) {
            if (inited) {
                try {
                    DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
                    int value = 0;
                    try {
                        value = args.getInt(3);
                    } catch (Exception e) {
                    }
                    String gtmVariable_target = (args.getString(0) != null ? args.getString(0) : "");
                    String gtmVariable_action = (args.getString(1) != null ? args.getString(1) : "");
                    String gtmVariable_targetProperties = (args.getString(2) != null ? args.getString(2) : "");

                    dataLayer.push(DataLayer.mapOf("event", "interaction", "target", gtmVariable_target, "action", gtmVariable_action, "target-properties", gtmVariable_targetProperties, "value", value));

                    callback.success("trackEvent - category = " + args.getString(0) + "; action = " + args.getString(1) + "; label = " + args.getString(2) + "; value = " + value);
                    return true;
                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("trackEvent failed - not initialized");
            }
        } else if (action.equals("pushEvent")) {
            if (inited) {
                try {
                    DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
                    dataLayer.push(stringMap(args.getJSONObject(0)));
                    callback.success("pushEvent: " + dataLayer.toString());
                    return true;
                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("pushEvent failed - not initialized");
            }
        } else if (action.equals("trackPage")) {
            if (inited) {
                try {
                    DataLayer dataLayer = TagManager.getInstance(this.cordova.getActivity().getApplicationContext()).getDataLayer();
                    dataLayer.push(DataLayer.mapOf("event", "content-view", "content-name", args.get(0)));
                    callback.success("trackPage - url = " + args.getString(0));
                    return true;
                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("trackPage failed - not initialized");
            }
        } else if (action.equals("dispatch")) {
            if (inited) {
                try {
                    GoogleAnalytics.getInstance(this.cordova.getActivity()).dispatchLocalHits();
                    callback.success("dispatch sent");
                    return true;
                } catch (final Exception e) {
                    callback.error(e.getMessage());
                }
            } else {
                callback.error("dispatch failed - not initialized");
            }
        }
        return false;
    }

    private Map<String, Object> stringMap(JSONObject o) throws JSONException {
        if (o.length() == 0) {
            return Collections.<String, Object>emptyMap();
        }
        Map<String, Object> map = new HashMap<String, Object>(o.length());
        Iterator it = o.keys();
        String key;
        Object value;
        while (it.hasNext()) {
            key = it.next().toString();
            value = o.has(key.toString()) ? o.get(key.toString()): null;
            map.put(key, value);
        }
        return map;
    }
}

