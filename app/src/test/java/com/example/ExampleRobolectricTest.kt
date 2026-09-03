package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.JarvisAction
import com.example.service.JarvisBrain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Jarvis AI", appName)
  }

  @Test
  fun `test jarvis brain parses phone call command in hinglish`() {
    val brain = JarvisBrain()
    val response = brain.parseCommandOffline("Rahul ko call karo")
    assertTrue(response.action is JarvisAction.MakeCall)
    assertEquals("Rahul", (response.action as JarvisAction.MakeCall).target)
  }

  @Test
  fun `test jarvis brain parses torch command`() {
    val brain = JarvisBrain()
    val response = brain.parseCommandOffline("flashlight on karo")
    assertTrue(response.action is JarvisAction.ToggleTorch)
    assertTrue((response.action as JarvisAction.ToggleTorch).enable)
  }

  @Test
  fun `test jarvis brain parses open app command`() {
    val brain = JarvisBrain()
    val response = brain.parseCommandOffline("WhatsApp kholo")
    assertTrue(response.action is JarvisAction.OpenApp)
    assertEquals("whatsapp", (response.action as JarvisAction.OpenApp).appName)
  }

  @Test
  fun `test jarvis brain parses english and hinglish open app commands`() {
    val brain = JarvisBrain()
    
    val r1 = brain.parseCommandOffline("open YouTube please")
    assertTrue(r1.action is JarvisAction.OpenApp)
    assertEquals("youtube", (r1.action as JarvisAction.OpenApp).appName)

    val r2 = brain.parseCommandOffline("Spotify chalao")
    assertTrue(r2.action is JarvisAction.OpenApp)
    assertEquals("spotify", (r2.action as JarvisAction.OpenApp).appName)

    val r3 = brain.parseCommandOffline("launch chrome")
    assertTrue(r3.action is JarvisAction.OpenApp)
    assertEquals("chrome", (r3.action as JarvisAction.OpenApp).appName)
  }

  @Test
  fun `test phone controller launchInstalledApp returns result`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val controller = com.example.service.PhoneController(context)
    val result = controller.launchInstalledApp("nonexistent_app_xyz")
    // Verifies the PackageManager execution path and intent resolution doesn't throw unhandled exceptions
    assertTrue(result.message.isNotEmpty())
  }
}

