package com.gdgtenerife.tlpandroidwear;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

/*
 * Activity Principal de la aplicación Movil
 * Clase encargada de mostrar el marcador y gestionar todos los eventos de sumar y restar puntos
 *
 */
public class MobilePrincipalActivity extends Activity {


    // Cogemos como nombre del TAG usando para los mensajes de Log, el nombre de esta misma clase.
    private static final String TAG = MobilePrincipalActivity.class.getSimpleName();
    // Objeto que nos comunicará con Google Play Services que es el encargado de gestionar todas las comunicaciones entre el SmartWatch y el Móvil.
    private GoogleApiClient apiClient;
    // Variables para tener almacenadas las puntuaciones en cada momento de ambos equipos, El Equipo A y el Equipo B
    private int scorea, scoreb;
    // Botones para modificar el marcador, Sumar y Restar puntos al equipo A y B.
    private Button addScoreA_btn;
    private Button addScoreB_btn;
    private Button subScoreA_btn;
    private Button subScoreB_btn;
    // Contenedores de Texto para mostrar las puntuaciones de ambos equipos.
    private TextView mTextViewScoreA, mTextViewScoreB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_principal);

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensureConnected();
    }

    // Para habilitar todos los botones (ponerlos visibles, están invisibles por defecto)
    // Se habilitarán cuando exista conexión entre el SmartWatch y el Movil
    private void setVisibilityAllButtons(){
        subScoreB_btn.setVisibility(View.VISIBLE);
        subScoreA_btn.setVisibility(View.VISIBLE);
        addScoreB_btn.setVisibility(View.VISIBLE);
        addScoreA_btn.setVisibility(View.VISIBLE);

    }

    // Función encargada de ver si existe comunicación entre el SmartWatch y el Movil... sino hay conexión, crea un nuevo canal de comunicación para establecerla
    private void ensureConnected() {
        if (apiClient != null && apiClient.isConnected()) {
            setVisibilityAllButtons();
        }
        else {
            apiClient = new GoogleApiClient.Builder(this, onConnectedListener, onConnectionListener).addApi(Wearable.API).build();
            apiClient.connect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sendCount(0, 0);
        handler.removeCallbacksAndMessages(handler);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.principal, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Función que se lanza cuando se crea una nueva vía de comunicación, un canal, entre el SmartWatch y el Movil
    private final GoogleApiClient.ConnectionCallbacks onConnectedListener = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            Log.d(TAG, "Connected, start sharing data.");
            scorea=0;
            scoreb=0;
            // Activamos el Listener que recibirá todos los cambios del contenedor de datos que usamos para compartir información,
            // o sea, se activará cada vez que el SmartWatch envíe algo al Móvil.
            Wearable.DataApi.addListener(apiClient, onDataChangedListener);
            setVisibilityAllButtons();
        }

        @Override
        public void onConnectionSuspended(int i) {
            cancelSend();
        }
    };

    // Evento que se lanza cuando el SmartWatch ha modificado los datos que estamos compartiendo, o sea
    // Envía información el SmartWatch al Móvil
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

    // Función encargada en Segundo Plano de actualizar los marcadores y mostrarlos por pantalla
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

    private void cancelSend() {
        handler.removeCallbacksAndMessages(handler);
    }

    private final Handler handler = new Handler();

    // Función encargada de modificar los datos compartidos con el SmartWatch, o sea
    // Enví info del Smartphone al SmartWatch
    private void sendCount(final int teama, final int teamb) {
        PutDataMapRequest dataMap = PutDataMapRequest.create("/count");
        dataMap.getDataMap().putInt("scorea", teama);
        dataMap.getDataMap().putInt("scoreb", teamb);
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(apiClient, request);
        Log.d(TAG, "Updating count to: " + teama + "-" + teamb);
    }

    // Evento encargado de gestionar una conexión NO exitosa.
    private final GoogleApiClient.OnConnectionFailedListener onConnectionListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.d(TAG, "Connection failed: " + connectionResult);
            showMessage("Connection failed: " + connectionResult);
            cancelSend();
        }
    };

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}