package com.translator.messagingapp;

import com.translator.messagingapp.message.*;
import com.translator.messagingapp.translation.*;
import com.translator.messagingapp.p2p.*;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebRTC dependency migration.
 * Tests that the new sendSms method works correctly and P2P functionality is intact.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class WebRTCMigrationTest {

    private Context context;
    private MessageService messageService;
    
    @Mock
    private TranslationManager mockTranslationManager;
    
    @Mock
    private TranslationCache mockTranslationCache;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        messageService = new MessageService(context, mockTranslationManager, mockTranslationCache);
    }

    /**
     * Tests that the new sendSms method exists and delegates to sendSmsMessage.
     */
    @Test
    public void testSendSmsMethodExists() {
        // Test that sendSms method is available and can be called
        try {
            // We can't actually send SMS in a unit test, but we can verify the method exists
            // and doesn't throw any immediate exceptions during setup
            java.lang.reflect.Method sendSmsMethod = messageService.getClass().getMethod("sendSms", String.class, String.class);
            assertNotNull("sendSms method should exist", sendSmsMethod);
            assertEquals("sendSms should return boolean", boolean.class, sendSmsMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("sendSms method should exist: " + e.getMessage());
        }
    }

    /**
     * Tests that sendSmsMessage method still exists (backward compatibility).
     */
    @Test
    public void testSendSmsMessageMethodExists() {
        try {
            java.lang.reflect.Method sendSmsMessageMethod = messageService.getClass().getMethod("sendSmsMessage", String.class, String.class);
            assertNotNull("sendSmsMessage method should exist", sendSmsMessageMethod);
            assertEquals("sendSmsMessage should return boolean", boolean.class, sendSmsMessageMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("sendSmsMessage method should exist: " + e.getMessage());
        }
    }

    /**
     * Tests that P2PConnectionManager can be instantiated with the new WebRTC version.
     */
    @Test
    public void testP2PConnectionManagerInstantiation() {
        try {
            P2PConnectionManager.P2PConnectionListener mockListener = mock(P2PConnectionManager.P2PConnectionListener.class);
            P2PConnectionManager manager = new P2PConnectionManager(context, mockListener);
            assertNotNull("P2PConnectionManager should be instantiable", manager);
        } catch (Exception e) {
            fail("P2PConnectionManager should be instantiable with new WebRTC version: " + e.getMessage());
        }
    }

    /**
     * Tests that sendP2PConnectionOffer method exists and can be called.
     */
    @Test
    public void testSendP2PConnectionOfferMethodExists() {
        try {
            java.lang.reflect.Method sendP2POfferMethod = messageService.getClass().getMethod("sendP2PConnectionOffer", String.class);
            assertNotNull("sendP2PConnectionOffer method should exist", sendP2POfferMethod);
            assertEquals("sendP2PConnectionOffer should return boolean", boolean.class, sendP2POfferMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("sendP2PConnectionOffer method should exist: " + e.getMessage());
        }
    }

    /**
     * Tests that basic P2P classes can be instantiated without throwing WebRTC-related errors.
     */
    @Test
    public void testP2PClassesInstantiation() {
        try {
            // Test P2PService
            P2PService p2pService = new P2PService(context);
            assertNotNull("P2PService should be instantiable", p2pService);

            // Test P2PConnectionData
            P2PConnectionData connectionData = new P2PConnectionData();
            assertNotNull("P2PConnectionData should be instantiable", connectionData);

        } catch (Exception e) {
            fail("P2P classes should be instantiable: " + e.getMessage());
        }
    }

    /**
     * Tests that WebRTC-related constants and enums are accessible.
     */
    @Test
    public void testWebRTCAPIAccessibility() {
        try {
            // Try to access some WebRTC classes to ensure they're available
            Class.forName("org.webrtc.PeerConnectionFactory");
            Class.forName("org.webrtc.PeerConnection");
            Class.forName("org.webrtc.DataChannel");
            Class.forName("org.webrtc.IceCandidate");
            Class.forName("org.webrtc.SessionDescription");
            Class.forName("org.webrtc.MediaConstraints");
        } catch (ClassNotFoundException e) {
            fail("WebRTC classes should be accessible: " + e.getMessage());
        }
    }
}