# Consent Reset UI Implementation Example

This document shows how to add a consent reset option in your app's settings screen.

## Option 1: Compose Implementation

Add this to your settings screen composable:

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.soccertips.predictx.App

@Composable
fun PrivacySettingsSection() {
    val context = LocalContext.current
    var showConsentDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Privacy & Consent",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Manage your ad personalization preferences",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            OutlinedButton(
                onClick = { showConsentDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Consent Preferences")
            }
            
            // Show consent status
            val app = context.applicationContext as App
            val canShowAds = app.canShowAds()
            
            Text(
                text = if (canShowAds) "Personalized ads: Enabled" else "Personalized ads: Disabled",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
    
    // Confirmation dialog
    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = { Text("Reset Consent Preferences") },
            text = { 
                Text("This will reset your ad personalization preferences and show the consent form again.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConsentDialog = false
                        val activity = context as? android.app.Activity
                        activity?.let {
                            (context.applicationContext as App).resetConsent(it)
                        }
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConsentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
```

## Option 2: Traditional Android View Implementation

Add this to your settings fragment:

```kotlin
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.soccertips.predictx.App
import com.soccertips.predictx.databinding.FragmentPrivacySettingsBinding

class PrivacySettingsFragment : Fragment() {
    
    private var _binding: FragmentPrivacySettingsBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrivacySettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set up reset consent button
        binding.resetConsentButton.setOnClickListener {
            showResetConsentDialog()
        }
        
        // Update consent status
        updateConsentStatus()
    }
    
    private fun showResetConsentDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reset Consent Preferences")
            .setMessage("This will reset your ad personalization preferences and show the consent form again.")
            .setPositiveButton("Reset") { dialog, _ ->
                val app = requireActivity().application as App
                app.resetConsent(requireActivity())
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun updateConsentStatus() {
        val app = requireActivity().application as App
        val canShowAds = app.canShowAds()
        
        binding.consentStatusText.text = if (canShowAds) {
            "Personalized ads: Enabled"
        } else {
            "Personalized ads: Disabled"
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

## Layout XML (fragment_privacy_settings.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="16dp">

    <com.google.android.material.card.MaterialCardView
        android:id="@+id/privacyCard"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:cardElevation="2dp"
        app:cardCornerRadius="8dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <TextView
                android:id="@+id/privacyTitle"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Privacy &amp; Consent"
                android:textAppearance="@style/TextAppearance.Material3.TitleMedium"
                android:layout_marginBottom="8dp"/>

            <TextView
                android:id="@+id/privacyDescription"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Manage your ad personalization preferences"
                android:textAppearance="@style/TextAppearance.Material3.BodySmall"
                android:textColor="?attr/colorOnSurfaceVariant"
                android:layout_marginBottom="16dp"/>

            <com.google.android.material.button.MaterialButton
                android:id="@+id/resetConsentButton"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Change Consent Preferences"
                style="@style/Widget.Material3.Button.OutlinedButton"/>

            <TextView
                android:id="@+id/consentStatusText"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Checking consent status..."
                android:textAppearance="@style/TextAppearance.Material3.BodySmall"
                android:textColor="?attr/colorOnSurfaceVariant"
                android:layout_marginTop="8dp"/>

        </LinearLayout>

    </com.google.android.material.card.MaterialCardView>

    <!-- Privacy Policy Link -->
    <com.google.android.material.button.MaterialButton
        android:id="@+id/privacyPolicyButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="View Privacy Policy"
        style="@style/Widget.Material3.Button.TextButton"
        app:layout_constraintTop_toBottomOf="@id/privacyCard"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="16dp"/>

</androidx.constraintlayout.widget.ConstraintLayout>
```

## Option 3: Simple Menu Item

Add this to your existing settings menu:

```kotlin
// In your settings activity or fragment
override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
        R.id.action_reset_consent -> {
            showResetConsentDialog()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}

private fun showResetConsentDialog() {
    AlertDialog.Builder(this)
        .setTitle("Reset Consent")
        .setMessage("Reset your ad personalization preferences?")
        .setPositiveButton("Reset") { _, _ ->
            (application as App).resetConsent(this)
        }
        .setNegativeButton("Cancel", null)
        .show()
}
```

## Testing the Reset Function

1. Launch the app and complete consent flow
2. Navigate to settings
3. Tap "Reset Consent Preferences"
4. Confirm the dialog
5. Consent form should appear again

## Best Practices

1. **Clear Labeling:** Use clear, user-friendly language
2. **Confirmation Dialog:** Always confirm before resetting
3. **Status Display:** Show current consent status
4. **Privacy Policy Link:** Include a link to your privacy policy
5. **Accessibility:** Ensure all elements are accessible

## String Resources (strings.xml)

```xml
<resources>
    <string name="privacy_consent_title">Privacy &amp; Consent</string>
    <string name="privacy_consent_description">Manage your ad personalization preferences</string>
    <string name="button_change_consent">Change Consent Preferences</string>
    <string name="button_privacy_policy">View Privacy Policy</string>
    <string name="consent_status_enabled">Personalized ads: Enabled</string>
    <string name="consent_status_disabled">Personalized ads: Disabled</string>
    <string name="dialog_reset_consent_title">Reset Consent Preferences</string>
    <string name="dialog_reset_consent_message">This will reset your ad personalization preferences and show the consent form again.</string>
    <string name="button_reset">Reset</string>
    <string name="button_cancel">Cancel</string>
</resources>
```

## Integration Checklist

- [ ] Add consent reset UI to settings screen
- [ ] Test reset functionality
- [ ] Verify consent form appears after reset
- [ ] Add privacy policy link
- [ ] Test on different devices
- [ ] Verify consent status updates correctly
- [ ] Add analytics tracking for consent resets (optional)

---

Choose the implementation that best fits your app's architecture (Compose or traditional Views).
