package com.example.curielabcompanion

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.curielabcompanion.databinding.ActivityMainBinding
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val decimalFormat = DecimalFormat("#.####") // For formatting output

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()

        binding.buttonCalculate.setOnClickListener {
            performCalculation()
        }

        // Optional: Clear result when inputs change to avoid confusion
        val inputWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.textViewResult.text = getString(R.string.result_placeholder) // Clear result
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }
        binding.inputStockConcentration.addTextChangedListener(inputWatcher)
        binding.inputFinalVolume.addTextChangedListener(inputWatcher)
        binding.inputDesiredConcentration.addTextChangedListener(inputWatcher)

        val spinnerWatcher = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.textViewResult.text = getString(R.string.result_placeholder) // Clear result
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.spinnerStockConcentrationUnit.onItemSelectedListener = spinnerWatcher
        binding.spinnerFinalVolumeUnit.onItemSelectedListener = spinnerWatcher
        binding.spinnerDesiredConcentrationUnit.onItemSelectedListener = spinnerWatcher

    }

    private fun setupSpinners() {
        ArrayAdapter.createFromResource(
            this,
            R.array.concentration_units,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerStockConcentrationUnit.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.volume_units,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerFinalVolumeUnit.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.concentration_units,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerDesiredConcentrationUnit.adapter = adapter
        }
    }

    private fun performCalculation() {
        // Clear previous errors
        binding.inputStockConcentration.error = null
        binding.inputFinalVolume.error = null
        binding.inputDesiredConcentration.error = null

        // 1. Read input values
        val stockConcStr = binding.inputStockConcentration.text.toString()
        val finalVolumeStr = binding.inputFinalVolume.text.toString()
        val desiredConcStr = binding.inputDesiredConcentration.text.toString()

        // 2. Validate inputs
        if (stockConcStr.isBlank()) {
            binding.inputStockConcentration.error = "Cannot be empty"
            return
        }
        if (finalVolumeStr.isBlank()) {
            binding.inputFinalVolume.error = "Cannot be empty"
            return
        }
        if (desiredConcStr.isBlank()) {
            binding.inputDesiredConcentration.error = "Cannot be empty"
            return
        }

        val stockConc = stockConcStr.toDoubleOrNull()
        val finalVolume = finalVolumeStr.toDoubleOrNull()
        val desiredConc = desiredConcStr.toDoubleOrNull()

        if (stockConc == null || stockConc <= 0) {
            binding.inputStockConcentration.error = "Invalid number"
            return
        }
        if (finalVolume == null || finalVolume <= 0) {
            binding.inputFinalVolume.error = "Invalid number"
            return
        }
        if (desiredConc == null || desiredConc < 0) { // Desired concentration can be 0
            binding.inputDesiredConcentration.error = "Invalid number"
            return
        }

        // 3. Get selected units
        val stockConcUnit = binding.spinnerStockConcentrationUnit.selectedItem.toString()
        val finalVolumeUnit = binding.spinnerFinalVolumeUnit.selectedItem.toString()
        val desiredConcUnit = binding.spinnerDesiredConcentrationUnit.selectedItem.toString()

        // 4. Convert to base units (M for concentration, L for volume)
        val c1Base = convertConcentrationToMolar(stockConc, stockConcUnit)
        val v2Base = convertVolumeToLiters(finalVolume, finalVolumeUnit)
        val c2Base = convertConcentrationToMolar(desiredConc, desiredConcUnit)

        if (c1Base == 0.0) {
            binding.textViewResult.text = "Error: Stock concentration (C1) cannot be zero."
            Toast.makeText(this, "Stock concentration cannot be zero after conversion.", Toast.LENGTH_LONG).show()
            return
        }

        // 5. Perform calculation: C1V1 = C2V2  => V1 = (C2 * V2) / C1
        val v1Base = (c2Base * v2Base) / c1Base

        // 6. Display result (V1) in a suitable unit
        val (resultVolume, resultUnit) = formatVolumeForDisplay(v1Base)
        binding.textViewResult.text = "Volume of stock needed (V1):\n${decimalFormat.format(resultVolume)} $resultUnit"
    }

    private fun convertConcentrationToMolar(value: Double, unit: String): Double {
        return when (unit) {
            "M" -> value
            "mM" -> value / 1000.0
            "µM" -> value / 1000000.0
            else -> value // Should not happen with controlled spinner values
        }
    }

    private fun convertVolumeToLiters(value: Double, unit: String): Double {
        return when (unit) {
            "L" -> value
            "mL" -> value / 1000.0
            "µL" -> value / 1000000.0
            else -> value // Should not happen
        }
    }

    private fun formatVolumeForDisplay(volumeInLiters: Double): Pair<Double, String> {
        return when {
            volumeInLiters >= 1.0 -> Pair(volumeInLiters, "L")
            volumeInLiters >= 0.001 -> Pair(volumeInLiters * 1000.0, "mL")
            else -> Pair(volumeInLiters * 1000000.0, "µL")
        }
    }
}