package com.glodanif.bluetoothchat.ui.presenter

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.bluetooth.BluetoothDevice
import com.glodanif.bluetoothchat.data.entity.ChatMessage
import com.glodanif.bluetoothchat.data.entity.Conversation
import com.glodanif.bluetoothchat.data.model.*
import com.glodanif.bluetoothchat.ui.view.ConversationsView
import com.glodanif.bluetoothchat.ui.viewmodel.ConversationViewModel
import com.glodanif.bluetoothchat.ui.viewmodel.converter.ConversationConverter
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

class ConversationsPresenter(private val view: ConversationsView,
                             private val connection: BluetoothConnector,
                             private val conversationStorage: ConversationsStorage,
                             private val settings: SettingsManager,
                             private val converter: ConversationConverter) : LifecycleObserver {

    private val prepareListener = object : OnPrepareListener {

        override fun onPrepared() {

            connection.addOnConnectListener(connectionListener)
            connection.addOnMessageListener(messageListener)

            loadConversations()

            val device = connection.getCurrentConversation()

            if (device != null && connection.isPending()) {
                view.notifyAboutConnectedDevice(converter.transform(device))
            } else {
                view.hideActions()
            }
        }

        override fun onError() {
            releaseConnection()
        }
    }

    private val connectionListener = object : OnConnectionListener {

        override fun onConnected(device: BluetoothDevice) {

        }

        override fun onConnectionWithdrawn() {
            view.hideActions()
        }

        override fun onConnectionDestroyed() {
            view.showServiceDestroyed()
        }

        override fun onConnectionAccepted() {
            view.refreshList(connection.getCurrentConversation()?.deviceAddress)
        }

        override fun onConnectionRejected() {

        }

        override fun onConnectedIn(conversation: Conversation) {
            view.notifyAboutConnectedDevice(converter.transform(conversation))
        }

        override fun onConnectedOut(conversation: Conversation) {
            view.redirectToChat(converter.transform(conversation))
        }

        override fun onConnecting() {

        }

        override fun onConnectionLost() {
            view.refreshList(connection.getCurrentConversation()?.deviceAddress)
            view.hideActions()
        }

        override fun onConnectionFailed() {
            view.refreshList(connection.getCurrentConversation()?.deviceAddress)
            view.hideActions()
        }

        override fun onDisconnected() {
            view.refreshList(connection.getCurrentConversation()?.deviceAddress)
            view.hideActions()
        }
    }

    private val messageListener = object : SimpleOnMessageListener() {

        override fun onMessageReceived(message: ChatMessage) {
            loadConversations()
        }

        override fun onMessageSent(message: ChatMessage) {
            loadConversations()
        }
    }

    fun loadConversations() = launch(UI) {

        val conversations = async(CommonPool) { conversationStorage.getConversations() }.await()

        if (conversations.isEmpty()) {
            view.showNoConversations()
        } else {
            val connectedDevice = if (connection.isConnected())
                connection.getCurrentConversation()?.deviceAddress else null
            view.showConversations(converter.transform(conversations), connectedDevice)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun loadUserProfile() {
        view.showUserProfile(settings.getUserName(), settings.getUserColor())
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun prepareConnection() {

        view.dismissConversationNotification()

        connection.addOnPrepareListener(prepareListener)

        if (connection.isConnectionPrepared()) {
            with(connection) {
                addOnConnectListener(connectionListener)
                addOnMessageListener(messageListener)
            }

            loadConversations()

            val device = connection.getCurrentConversation()

            if (device != null && connection.isPending()) {
                view.notifyAboutConnectedDevice(converter.transform(device))
            } else {
                view.hideActions()
            }
        } else {
            connection.prepare()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun releaseConnection() {
        with(connection) {
            removeOnPrepareListener(prepareListener)
            removeOnConnectListener(connectionListener)
            removeOnMessageListener(messageListener)
        }
    }

    fun startChat(conversation: ConversationViewModel) {
        connection.acceptConnection()
        view.hideActions()
        view.redirectToChat(conversation)
    }

    fun rejectConnection() {
        view.hideActions()
        connection.rejectConnection()
    }

    fun removeConversation(address: String) {
        connection.sendDisconnectRequest()
        launch {
            conversationStorage.removeConversationByAddress(address)
        }
        view.removeFromShortcuts(address)
        loadConversations()
    }

    fun disconnect() {
        connection.sendDisconnectRequest()
        loadConversations()
    }
}
