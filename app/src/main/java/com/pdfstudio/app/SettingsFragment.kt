package com.pdfstudio.app

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.pdfstudio.app.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("pdf_studio_prefs", Context.MODE_PRIVATE)

        // Privacy Policy button
        binding.btnPrivacyPolicy?.setOnClickListener {
            showPrivacyPolicy()
        }

        // Rate Us button
        binding.btnRateUs?.setOnClickListener {
            try {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("market://details?id=" + requireContext().packageName)
                )
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + requireContext().packageName)
                    )
                    startActivity(intent)
                } catch (e2: Exception) {
                    android.widget.Toast.makeText(requireContext(), "Could not open Play Store", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showPrivacyPolicy() {
        AlertDialog.Builder(requireContext())
            .setTitle("Privacy Policy")
            .setMessage(getString(R.string.privacy_policy_text))
            .setPositiveButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
