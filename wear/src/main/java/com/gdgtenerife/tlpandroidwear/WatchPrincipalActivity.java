package com.gdgtenerife.tlpandroidwear;

import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.InsetActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class WatchPrincipalActivity extends InsetActivity {

    private static final String TAG = WatchPrincipalActivity.class.getSimpleName();
    private GoogleApiClient apiClient;
    private final Handler handler = new Handler();
    private int scorea, scoreb;
    private Button addScoreA_btn;
    private Button addScoreB_btn;
    private Button subScoreA_btn;
    private Button subScoreB_btn;
    private TextView mTextViewScoreA, mTextViewScoreB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiClient = new GoogleApiClient.Builder(this, onConnectedListener, onConnectionListener).addApi(Wearable.API).build();
        apiClient.connect();
    }

    private void sendCount(final int teama, final int teamb) {
        PutDataMapRequest dataMap = PutDataMapRequest.create("/count");
        dataMap.getDataMap().putInt("scorea", teama);
        dataMap.getDataMap().putInt("scoreb", teamb);
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(apiClient, request);
        Log.d(TAG, "Updating count to: " + teama + "-" + teamb);
    }

    // Función que se lanza cuando la aplicación del SmartWatch está corriendo en primer plano
    @Override
    public void onReadyForContent() {
        setContentView(R.layout.activity_watch_principal);
        mTextViewScoreA = (TextView) findViewById(R.id.scorea);
        mTextViewScoreB = (TextView) findViewById(R.id.scoreb);

        addScoreA_btn = (Button) findViewById(R.id.adda);
        addScoreB_btn = (Button) findViewById(R.id.addb);
        subScoreA_btn = (Button) findViewById(R.id.suba);
        subScoreB_btn = (Button) findViewById(R.id.subb);

        addScoreA_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCount(++scorea, scoreb);
            }
        });

        addScoreB_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCount(scorea, ++scoreb);
            }
        });

        subScoreA_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scorea > 0)
                    sendCount(--scorea, scoreb);
            }
        });

        subScoreB_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scoreb > 0)
                    sendCount(scorea, --scoreb);
            }
        });

        Log.d(TAG, "Watch ready for content!");
    }

    private void setVisibilityAllButtons(){
        subScoreB_btn.setVisibility(View.VISIBLE);
        subScoreA_btn.setVisibility(View.VISIBLE);
        addScoreB_btn.setVisibility(View.VISIBLE);
        addScoreA_btn.setVisibility(View.VISIBLE);

    }

    private final GoogleApiClient.ConnectionCallbacks onConnectedListener = new GoogleApiClient.ConnectionCallbacks() {

        @Override
        public void onConnected(Bundle bundle) {
            Log.d(TAG, "Connected, start listening for data!");
            Wearable.DataApi.addListener(apiClient, onDataChangedListener);
            setVisibilityAllButtons();
        }

        @Override
        public void onConnectionSuspended(int i) {
        }
    };

    private final GoogleApiClient.OnConnectionFailedListener onConnectionListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.d(TAG, "Connection failed: " + connectionResult);
        }
    };

    public DataApi.DataListener onDataChangedListener = new DataApi.DataListener() {
        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            Log.d(TAG, "Data changed: " + dataEvents);
            for (DataEvent event : dataEvents) {
                if (event.getType() == DataEvent.TYPE_DELETED) {
                    Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
                    handler.post(onNewCount(0, 0));
                } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                    Log.d(TAG, "DataItem changed: " + event.getDataItem().getUri());
                    if (event.getDataItem().getUri().getPath().endsWith("/count")) {
                        DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                        int teama = dataMapItem.getDataMap().getInt("scorea");
                        int teamb = dataMapItem.getDataMap().getInt("scoreb");
                        handler.post(onNewCount(teama, teamb));
                    }
                }
            }
        }
    };

    private Runnable onNewCount(final int teama, final int teamb) {
        return new Runnable() {
            @Override
            public void run() {
                if ((mTextViewScoreA != null) && (mTextViewScoreB != null)) {
                    scorea = teama;
                    scoreb = teamb;
                    mTextViewScoreA.setText(Integer.toString(teama));
                    mTextViewScoreB.setText(Integer.toString(teamb));
                }
            }
        };
    }
}