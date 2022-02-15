package com.google.location.nearby.apps.walkietalkie;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.google.location.nearby.apps.walkietalkie.Constants.TAG;

/** A class that connects to Nearby Connections and provides convenience methods and callbacks. */
public abstract class ConnectionsActivity extends AppCompatActivity {

  /**
   * These permissions are required before connecting to Nearby Connections.
   */
  private static final String[] REQUIRED_PERMISSIONS;
  static {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      REQUIRED_PERMISSIONS =
          new String[] {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
          };
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      REQUIRED_PERMISSIONS =
          new String[] {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
          };
    } else {
      REQUIRED_PERMISSIONS =
          new String[] {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
          };
    }
  }

  private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

  /** Our handler to Nearby Connections. */
  private ConnectionsClient mConnectionsClient;

  /** The devices we've discovered near us. */
  private final Map<String, Endpoint> mDiscoveredEndpoints = new HashMap<>();

  /**
   * The devices we have pending connections to. They will stay pending until we call {@link
   * #acceptConnection(Endpoint)} or {@link #rejectConnection(Endpoint)}.
   */
  private final Map<String, Endpoint> mPendingConnections = new HashMap<>();

  /**
   * The devices we are currently connected to. For advertisers, this may be large. For discoverers,
   * there will only be one entry in this map.
   */
  private final Map<String, Endpoint> mEstablishedConnections = new HashMap<>();

  /**
   * True if we are asking a discovered device to connect to us. While we ask, we cannot ask another
   * device.
   */
  private boolean mIsConnecting = false;

  /** True if we are discovering. */
  private boolean mIsDiscovering = false;

  /** True if we are advertising. */
  private boolean mIsAdvertising = false;

  /** Callbacks for connections to other devices. */
  private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
      new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
          logD(
              String.format(
                  "onConnectionInitiated(endpointId=%s, endpointName=%s)",
                  endpointId, connectionInfo.getEndpointName()));
          Endpoint endpoint = new Endpoint(endpointId, connectionInfo.getEndpointName());
          mPendingConnections.put(endpointId, endpoint);
          ConnectionsActivity.this.onConnectionInitiated(endpoint, connectionInfo);
        }

        @Override
        public void onConnectionResult(String endpointId, ConnectionResolution result) {
          logD(String.format("onConnectionResponse(endpointId=%s, result=%s)", endpointId, result));

          // We're no longer connecting
          mIsConnecting = false;

          if (!result.getStatus().isSuccess()) {
            logW(
                String.format(
                    "Connection failed. Received status %s.",
                    ConnectionsActivity.toString(result.getStatus())));
            onConnectionFailed(mPendingConnections.remove(endpointId));
            return;
          }
          connectedToEndpoint(mPendingConnections.remove(endpointId));
        }

        @Override
        public void onDisconnected(String endpointId) {
          if (!mEstablishedConnections.containsKey(endpointId)) {
            logW("Unexpected disconnection from endpoint " + endpointId);
            return;
          }
          disconnectedFromEndpoint(mEstablishedConnections.get(endpointId));
        }
      };

  /** Callbacks for payloads (bytes of data) sent from another device to us. */
  private final PayloadCallback mPayloadCallback =
      new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
          logD(String.format("onPayloadReceived(endpointId=%s, payload=%s)", endpointId, payload));
          onReceive(mEstablishedConnections.get(endpointId), payload);
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
          logD(
              String.format(
                  "onPayloadTransferUpdate(endpointId=%s, update=%s)", endpointId, update));
        }
      };

  /** Called when our Activity is first created. */
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mConnectionsClient = Nearby.getConnectionsClient(this);
  }

  /** Called when our Activity has been made visible to the user. */
  @Override
  protected void onStart() {
    super.onStart();
    if (!hasPermissions(this, getRequiredPermissions())) {
      if (Build.VERSION.SDK_INT < 23) {
        ActivityCompat.requestPermissions(
            this, getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
      } else {
        requestPermissions(getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
      }
    }
  }

  /** Called when the user has accepted (or denied) our permission request. */
  @CallSuper
  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
      int i = 0;
      for (int grantResult : grantResults) {
        if (grantResult == PackageManager.PERMISSION_DENIED) {
          logW("Failed to request the permission " + permissions[i]);
          Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
          finish();
          return;
        }
        i++;
      }
      recreate();
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  /**
   * Sets the device to advertising mode. It will broadcast to other devices in discovery mode.
   * Either {@link #onAdvertisingStarted()} or {@link #onAdvertisingFailed()} will be called once
   * we've found out if we successfully entered this mode.
   */
  protected void startAdvertising() {
    mIsAdvertising = true;
    final String localEndpointName = getName();

    AdvertisingOptions.Builder advertisingOptions = new AdvertisingOptions.Builder();
    advertisingOptions.setStrategy(getStrategy());

    mConnectionsClient
        .startAdvertising(
            localEndpointName,
            getServiceId(),
            mConnectionLifecycleCallback,
            advertisingOptions.build())
        .addOnSuccessListener(
            new OnSuccessListener<Void>() {
              @Override
              public void onSuccess(Void unusedResult) {
                logV("Now advertising endpoint " + localEndpointName);
                onAdvertisingStarted();
              }
            })
        .addOnFailureListener(
            new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                mIsAdvertising = false;
                logW("startAdvertising() failed.", e);
                onAdvertisingFailed();
              }
            });
  }

  /** Stops advertising. */
  protected void stopAdvertising() {
    mIsAdvertising = false;
    mConnectionsClient.stopAdvertising();
  }

  /** Returns {@code true} if currently advertising. */
  protected boolean isAdvertising() {
    return mIsAdvertising;
  }

  /** Called when advertising successfully starts. Override this method to act on the event. */
  protected void onAdvertisingStarted() {}

  /** Called when advertising fails to start. Override this method to act on the event. */
  protected void onAdvertisingFailed() {}

  /**
   * Called when a pending connection with a remote endpoint is created. Use {@link ConnectionInfo}
   * for metadata about the connection (like incoming vs outgoing, or the authentication token). If
   * we want to continue with the connection, call {@link #acceptConnection(Endpoint)}. Otherwise,
   * call {@link #rejectConnection(Endpoint)}.
   */
  protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {}

  /** Accepts a connection request. */
  protected void acceptConnection(final Endpoint endpoint) {
    mConnectionsClient
        .acceptConnection(endpoint.getId(), mPayloadCallback)
        .addOnFailureListener(
            new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                logW("acceptConnection() failed.", e);
              }
            });
  }

  /** Rejects a connection request. */
  protected void rejectConnection(Endpoint endpoint) {
    mConnectionsClient
        .rejectConnection(endpoint.getId())
        .addOnFailureListener(
            new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                logW("rejectConnection() failed.", e);
              }
            });
  }

  /**
   * Sets the device to discovery mode. It will now listen for devices in advertising mode. Either
   * {@link #onDiscoveryStarted()} or {@link #onDiscoveryFailed()} will be called once we've found
   * out if we successfully entered this mode.
   */
  protected void startDiscovering() {
    mIsDiscovering = true;
    mDiscoveredEndpoints.clear();
    DiscoveryOptions.Builder discoveryOptions = new DiscoveryOptions.Builder();
    discoveryOptions.setStrategy(getStrategy());
    mConnectionsClient
        .startDiscovery(
            getServiceId(),
            new EndpointDiscoveryCallback() {
              @Override
              public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                logD(
                    String.format(
                        "onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)",
                        endpointId, info.getServiceId(), info.getEndpointName()));

                if (getServiceId().equals(info.getServiceId())) {
                  Endpoint endpoint = new Endpoint(endpointId, info.getEndpointName());
                  mDiscoveredEndpoints.put(endpointId, endpoint);
                  onEndpointDiscovered(endpoint);
                }
              }

              @Override
              public void onEndpointLost(String endpointId) {
                logD(String.format("onEndpointLost(endpointId=%s)", endpointId));
              }
            },
            discoveryOptions.build())
        .addOnSuccessListener(
            new OnSuccessListener<Void>() {
              @Override
              public void onSuccess(Void unusedResult) {
                onDiscoveryStarted();
              }
            })
        .addOnFailureListener(
            new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                mIsDiscovering = false;
                logW("startDiscovering() failed.", e);
                onDiscoveryFailed();
              }
            });
  }

  /** Stops discovery. */
  protected void stopDiscovering() {
    mIsDiscovering = false;
    mConnectionsClient.stopDiscovery();
  }

  /** Returns {@code true} if currently discovering. */
  protected boolean isDiscovering() {
    return mIsDiscovering;
  }

  /** Called when discovery successfully starts. Override this method to act on the event. */
  protected void onDiscoveryStarted() {}

  /** Called when discovery fails to start. Override this method to act on the event. */
  protected void onDiscoveryFailed() {}

  /**
   * Called when a remote endpoint is discovered. To connect to the device, call {@link
   * #connectToEndpoint(Endpoint)}.
   */
  protected void onEndpointDiscovered(Endpoint endpoint) {}

  /** Disconnects from the given endpoint. */
  protected void disconnect(Endpoint endpoint) {
    mConnectionsClient.disconnectFromEndpoint(endpoint.getId());
    mEstablishedConnections.remove(endpoint.getId());
  }

  /** Disconnects from all currently connected endpoints. */
  protected void disconnectFromAllEndpoints() {
    for (Endpoint endpoint : mEstablishedConnections.values()) {
      mConnectionsClient.disconnectFromEndpoint(endpoint.getId());
    }
    mEstablishedConnections.clear();
  }

  /** Resets and clears all state in Nearby Connections. */
  protected void stopAllEndpoints() {
    mConnectionsClient.stopAllEndpoints();
    mIsAdvertising = false;
    mIsDiscovering = false;
    mIsConnecting = false;
    mDiscoveredEndpoints.clear();
    mPendingConnections.clear();
    mEstablishedConnections.clear();
  }

  /**
   * Sends a connection request to the endpoint. Either {@link #onConnectionInitiated(Endpoint,
   * ConnectionInfo)} or {@link #onConnectionFailed(Endpoint)} will be called once we've found out
   * if we successfully reached the device.
   */
  protected void connectToEndpoint(final Endpoint endpoint) {
    logV("Sending a connection request to endpoint " + endpoint);
    // Mark ourselves as connecting so we don't connect multiple times
    mIsConnecting = true;

    // Ask to connect
    mConnectionsClient
        .requestConnection(getName(), endpoint.getId(), mConnectionLifecycleCallback)
        .addOnFailureListener(
            new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                logW("requestConnection() failed.", e);
                mIsConnecting = false;
                onConnectionFailed(endpoint);
              }
            });
  }

  /** Returns {@code true} if we're currently attempting to connect to another device. */
  protected final boolean isConnecting() {
    return mIsConnecting;
  }

  private void connectedToEndpoint(Endpoint endpoint) {
    logD(String.format("connectedToEndpoint(endpoint=%s)", endpoint));
    mEstablishedConnections.put(endpoint.getId(), endpoint);
    onEndpointConnected(endpoint);
  }

  private void disconnectedFromEndpoint(Endpoint endpoint) {
    logD(String.format("disconnectedFromEndpoint(endpoint=%s)", endpoint));
    mEstablishedConnections.remove(endpoint.getId());
    onEndpointDisconnected(endpoint);
  }

  /**
   * Called when a connection with this endpoint has failed. Override this method to act on the
   * event.
   */
  protected void onConnectionFailed(Endpoint endpoint) {}

  /** Called when someone has connected to us. Override this method to act on the event. */
  protected void onEndpointConnected(Endpoint endpoint) {}

  /** Called when someone has disconnected. Override this method to act on the event. */
  protected void onEndpointDisconnected(Endpoint endpoint) {}

  /** Returns a list of currently connected endpoints. */
  protected Set<Endpoint> getDiscoveredEndpoints() {
    return new HashSet<>(mDiscoveredEndpoints.values());
  }

  /** Returns a list of currently connected endpoints. */
  protected Set<Endpoint> getConnectedEndpoints() {
    return new HashSet<>(mEstablishedConnections.values());
  }

  /**
   * Sends a {@link Payload} to all currently connected endpoints.
   *
   * @param payload The data you want to send.
   */
  protected void send(Payload payload) {
    send(payload, mEstablishedConnections.keySet());
  }

  private void send(Payload payload, Set<String> endpoints) {
    mConnectionsClient
        .sendPayload(new ArrayList<>(endpoints), payload)
        .addOnFailureListener(
            new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                logW("sendPayload() failed.", e);
              }
            });
  }

  /**
   * Someone connected to us has sent us data. Override this method to act on the event.
   *
   * @param endpoint The sender.
   * @param payload The data.
   */
  protected void onReceive(Endpoint endpoint, Payload payload) {}

  /**
   * An optional hook to pool any permissions the app needs with the permissions ConnectionsActivity
   * will request.
   *
   * @return All permissions required for the app to properly function.
   */
  protected String[] getRequiredPermissions() {
    return REQUIRED_PERMISSIONS;
  }

  /** Returns the client's name. Visible to others when connecting. */
  protected abstract String getName();

  /**
   * Returns the service id. This represents the action this connection is for. When discovering,
   * we'll verify that the advertiser has the same service id before we consider connecting to them.
   */
  protected abstract String getServiceId();

  /**
   * Returns the strategy we use to connect to other devices. Only devices using the same strategy
   * and service id will appear when discovering. Stragies determine how many incoming and outgoing
   * connections are possible at the same time, as well as how much bandwidth is available for use.
   */
  protected abstract Strategy getStrategy();

  /**
   * Transforms a {@link Status} into a English-readable message for logging.
   *
   * @param status The current status
   * @return A readable String. eg. [404]File not found.
   */
  private static String toString(Status status) {
    return String.format(
        Locale.US,
        "[%d]%s",
        status.getStatusCode(),
        status.getStatusMessage() != null
            ? status.getStatusMessage()
            : ConnectionsStatusCodes.getStatusCodeString(status.getStatusCode()));
  }

  /**
   * Returns {@code true} if the app was granted all the permissions. Otherwise, returns {@code
   * false}.
   */
  public static boolean hasPermissions(Context context, String... permissions) {
    for (String permission : permissions) {
      if (ContextCompat.checkSelfPermission(context, permission)
          != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  @CallSuper
  protected void logV(String msg) {
    Log.v(TAG, msg);
  }

  @CallSuper
  protected void logD(String msg) {
    Log.d(TAG, msg);
  }

  @CallSuper
  protected void logW(String msg) {
    Log.w(TAG, msg);
  }

  @CallSuper
  protected void logW(String msg, Throwable e) {
    Log.w(TAG, msg, e);
  }

  @CallSuper
  protected void logE(String msg, Throwable e) {
    Log.e(TAG, msg, e);
  }

  /** Represents a device we can talk to. */
  protected static class Endpoint {
    @NonNull private final String id;
    @NonNull private final String name;

    private Endpoint(@NonNull String id, @NonNull String name) {
      this.id = id;
      this.name = name;
    }

    @NonNull
    public String getId() {
      return id;
    }

    @NonNull
    public String getName() {
      return name;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Endpoint) {
        Endpoint other = (Endpoint) obj;
        return id.equals(other.id);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }

    @Override
    public String toString() {
      return String.format("Endpoint{id=%s, name=%s}", id, name);
    }
  }
}
