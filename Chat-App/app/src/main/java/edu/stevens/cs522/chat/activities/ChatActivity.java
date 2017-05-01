/*********************************************************************

    Chat server: accept chat messages from clients.
    
    Sender chatName and GPS coordinates are encoded
    in the messages, and stripped off upon receipt.

    Copyright (c) 2017 Stevens Institute of Technology

**********************************************************************/
package edu.stevens.cs522.chat.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import edu.stevens.cs522.chat.R;
import edu.stevens.cs522.chat.adapter.MessageCursorAdapter;
import edu.stevens.cs522.chat.async.QueryBuilder;
import edu.stevens.cs522.chat.contracts.MessageContract;
import edu.stevens.cs522.chat.entities.ChatMessage;
import edu.stevens.cs522.chat.managers.MessageManager;
import edu.stevens.cs522.chat.managers.PeerManager;
import edu.stevens.cs522.chat.managers.TypedCursor;
import edu.stevens.cs522.chat.rest.ChatHelper;
import edu.stevens.cs522.chat.rest.ServiceManager;
import edu.stevens.cs522.chat.settings.Settings;
import edu.stevens.cs522.chat.util.InetAddressUtils;
import edu.stevens.cs522.chat.util.ResultReceiverWrapper;

public class ChatActivity extends Activity implements OnClickListener, QueryBuilder.IQueryListener<ChatMessage>, ResultReceiverWrapper.IReceive {

	final static public String TAG_LOG = ChatActivity.class.getSimpleName();
		
    /*
     * UI for displaying received messages
     */
//	private SimpleCursorAdapter messages;

    @BindView(R.id.message_list) ListView messageList;

//    private SimpleCursorAdapter messagesAdapter;
    private MessageCursorAdapter messagesAdapter;

    private MessageManager messageManager;

    private PeerManager peerManager;

    private ServiceManager serviceManager;

    /*
     * Widgets for dest address, message text, send button.
     */
    @BindView(R.id.chat_room) EditText chatRoomName;
    @BindView(R.id.message_text) EditText messageText;
    @BindView(R.id.send_button) Button sendButton;
    @BindView(R.id.empty_view) TextView emptyView;

    /*
     * Helper for Web service
     */
    private ChatHelper helper;

    /*
     * For receiving ack when message is sent.
     */
    private ResultReceiverWrapper sendResultReceiver;
	
	/*
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        /**
         * Initialize settings to default values.
         */
        if (!Settings.isRegistered(this)) {
            // Launch registration activity
            Settings.getClientId(this);
            startActivity(new Intent(this, RegisterActivity.class));
        }

        setContentView(R.layout.messages);
        ButterKnife.bind(this);
        sendButton.setOnClickListener(this);

        // COMPLETED use SimpleCursorAdapter to display the messages received.
        messagesAdapter = new MessageCursorAdapter(this, null);
        messageList.setAdapter(messagesAdapter);
        messageList.setEmptyView(emptyView);

        // COMPLETED create the message and peer managers, and initiate a query for all messages
        messageManager = new MessageManager(this);
        messageManager.getAllMessagesAsync(this);
        peerManager = new PeerManager(this);

        // COMPLETED instantiate helper for service
        helper = new ChatHelper(this);

        // COMPLETED initialize sendResultReceiver and serviceManager
        sendResultReceiver = new ResultReceiverWrapper(new Handler());
        serviceManager = new ServiceManager(this);
    }

	public void onResume() {
        super.onResume();
        sendResultReceiver.setReceiver(this);
        serviceManager.scheduleBackgroundOperations();
    }

    public void onPause() {
        super.onPause();
        sendResultReceiver.setReceiver(null);
        serviceManager.cancelBackgroundOperations();
    }

    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // COMPLETED inflate a menu with PEERS and SETTINGS options
        getMenuInflater().inflate(R.menu.chatserver_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(item.getItemId()) {

            // COMPLETED PEERS provide the UI for viewing list of peers
            case R.id.peers:
                Intent intentForPeers = new Intent(this, ViewPeersActivity.class);
                startActivity(intentForPeers);
                break;

            // COMPLETED SETTINGS provide the UI for settings
            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;

            default:
        }
        return false;
    }

    /*
     * Callback for the SEND button.
     */
    public void onClick(View v) {
        if (helper != null) {

            String chatRoom;

            String message = null;

            // COMPLETED get chatRoom and message from UI, and use helper to post a message
            chatRoom = chatRoomName.getText().toString();
            Log.v(TAG_LOG, "chatRoom: " + chatRoom);
            message = messageText.getText().toString();

            helper.postMessage(chatRoom, message, sendResultReceiver);

            Log.i(TAG_LOG, "Sent message: " + message);

            messageText.setText("");
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle data) {
        switch (resultCode) {
            case RESULT_OK:
                // COMPLETED show a success toast message
                Toast.makeText(this, "Success in Main", Toast.LENGTH_SHORT).show();
                break;

            default:
                // COMPLETED show a failure toast message
                Toast.makeText(this, "Fail in Main", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void handleResults(TypedCursor<ChatMessage> results) {
        // COMPLETED
        messagesAdapter.swapCursor(results.getCursor());
    }

    @Override
    public void closeResults() {
        // COMPLETED
        messagesAdapter.swapCursor(null);
    }

}