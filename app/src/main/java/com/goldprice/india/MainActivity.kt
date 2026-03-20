package com.goldprice.india

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

data class City(val name: String, val url: String)
data class GoldPrice(val karat: String, val purity: String, val perGram: String, val per10g: String)

class MainActivity : AppCompatActivity() {

    private lateinit var hiddenWebView: WebView  // silent fetcher, never shown
    private lateinit var progressBar: ProgressBar
    private lateinit var spinner: Spinner
    private lateinit var btnRefresh: ImageButton
    private lateinit var priceContainer: LinearLayout
    private lateinit var tvLastUpdated: TextView
    private lateinit var tvError: TextView

    private val cities = listOf(
        City("India (National)", "https://www.goodreturns.in/gold-rates/"),
        City("Mumbai",        "https://www.goodreturns.in/gold-rates/mumbai.html"),
        City("Delhi",         "https://www.goodreturns.in/gold-rates/delhi.html"),
        City("Bangalore",     "https://www.goodreturns.in/gold-rates/bangalore.html"),
        City("Chennai",       "https://www.goodreturns.in/gold-rates/chennai.html"),
        City("Hyderabad",     "https://www.goodreturns.in/gold-rates/hyderabad.html"),
        City("Pune",          "https://www.goodreturns.in/gold-rates/pune.html"),
        City("Kolkata",       "https://www.goodreturns.in/gold-rates/kolkata.html"),
        City("Ahmedabad",     "https://www.goodreturns.in/gold-rates/ahmedabad.html"),
        City("Jaipur",        "https://www.goodreturns.in/gold-rates/jaipur.html"),
        City("Noida",         "https://www.goodreturns.in/gold-rates/noida.html"),
        City("Lucknow",       "https://www.goodreturns.in/gold-rates/lucknow.html"),
        City("Surat",         "https://www.goodreturns.in/gold-rates/surat.html"),
        City("Vijayawada",    "https://www.goodreturns.in/gold-rates/vijayawada.html"),
    )

    private var selectedCityIndex = 0

    // JS that extracts ONLY gold price numbers from the DOM and sends back to Android
    private val extractJS = """
        (function() {
            try {
                var results = [];
                var karatMap = {'24': {purity:'99.9%'}, '22': {purity:'91.6%'}, '18': {purity:'75.0%'}};
                
                // Try to find price rows - GoodReturns uses various table structures
                var rows = document.querySelectorAll('tr, .gold-rate-row, .rate-row');
                rows.forEach(function(row) {
                    var text = row.innerText || '';
                    var karat = null;
                    if (text.match(/24\s*K/i) || text.match(/24\s*carat/i)) karat = '24';
                    else if (text.match(/22\s*K/i) || text.match(/22\s*carat/i)) karat = '22';
                    else if (text.match(/18\s*K/i) || text.match(/18\s*carat/i)) karat = '18';
                    if (!karat) return;
                    
                    // Extract all numbers from this row
                    var nums = text.match(/[\d,]{4,}/g) || [];
                    nums = nums.map(function(n){ return n.replace(/,/g,''); })
                               .map(Number)
                               .filter(function(n){ return n > 2000 && n < 200000; });
                    
                    if (nums.length >= 1) {
                        var perGram = nums.find(function(n){ return n > 2000 && n < 20000; }) || nums[0];
                        var per10g  = nums.find(function(n){ return n > 20000 && n < 200000; }) || (perGram * 10);
                        results.push({
                            karat: karat + 'K',
                            purity: karatMap[karat].purity,
                            perGram: '₹' + perGram.toLocaleString('en-IN'),
                            per10g:  '₹' + per10g.toLocaleString('en-IN')
                        });
                    }
                });
                
                // Deduplicate by karat
                var seen = {};
                results = results.filter(function(r){
                    if (seen[r.karat]) return false;
                    seen[r.karat] = true;
                    return true;
                });
                
                Android.onPricesExtracted(JSON.stringify(results));
            } catch(e) {
                Android.onError('Parse error: ' + e.message);
            }
        })();
    """.trimIndent()

    inner class AndroidBridge {
        @JavascriptInterface
        fun onPricesExtracted(json: String) {
            runOnUiThread { displayPrices(json) }
        }

        @JavascriptInterface
        fun onError(msg: String) {
            runOnUiThread { showError(msg) }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar   = findViewById(R.id.progress_bar)
        spinner       = findViewById(R.id.spinner_city)
        btnRefresh    = findViewById(R.id.btn_refresh)
        priceContainer= findViewById(R.id.price_container)
        tvLastUpdated = findViewById(R.id.tv_last_updated)
        tvError       = findViewById(R.id.tv_error)
        hiddenWebView = findViewById(R.id.hidden_webview)

        setupHiddenWebView()
        setupSpinner()
        btnRefresh.setOnClickListener { loadCity(cities[selectedCityIndex]) }

        loadCity(cities[0])
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupHiddenWebView() {
        hiddenWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
        }
        hiddenWebView.addJavascriptInterface(AndroidBridge(), "Android")
        hiddenWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                // Page loaded silently — now extract just the price data
                view?.evaluateJavascript(extractJS, null)
            }
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cities.map { it.name })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos != selectedCityIndex) { selectedCityIndex = pos; loadCity(cities[pos]) }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun loadCity(city: City) {
        progressBar.visibility = View.VISIBLE
        tvError.visibility = View.GONE
        priceContainer.removeAllViews()
        hiddenWebView.loadUrl(city.url)  // loads silently in background
    }

    private fun displayPrices(json: String) {
        progressBar.visibility = View.GONE
        priceContainer.removeAllViews()

        try {
            val org = org.json.JSONArray(json)
            if (org.length() == 0) { showError("No prices found. Try refreshing."); return }

            val cityName = cities[selectedCityIndex].name
            val header = TextView(this).apply {
                text = "Gold Prices — $cityName"
                textSize = 16f; setTextColor(Color.parseColor("#FFD700"))
                setPadding(0, 0, 0, 16)
            }
            priceContainer.addView(header)

            // Column headers
            priceContainer.addView(makeRow("Karat", "Purity", "Per Gram", "Per 10g", isHeader = true))

            for (i in 0 until org.length()) {
                val item = org.getJSONObject(i)
                priceContainer.addView(makeRow(
                    item.getString("karat"),
                    item.getString("purity"),
                    item.getString("perGram"),
                    item.getString("per10g"),
                    isHeader = false,
                    alternate = i % 2 == 1
                ))
            }

            val now = java.text.SimpleDateFormat("hh:mm a, dd MMM yyyy", java.util.Locale.getDefault())
                .format(java.util.Date())
            tvLastUpdated.text = "Updated: $now · Source: GoodReturns.in"
            tvLastUpdated.visibility = View.VISIBLE

        } catch (e: Exception) {
            showError("Failed to parse prices. Please refresh.")
        }
    }

    private fun makeRow(c1: String, c2: String, c3: String, c4: String,
                        isHeader: Boolean, alternate: Boolean = false): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor(when {
                isHeader  -> "#2a1800"
                alternate -> "#1a1200"
                else      -> "#0f0b00"
            }))
            setPadding(0, 2, 0, 2)
        }
        listOf(c1 to 1f, c2 to 1f, c3 to 1.2f, c4 to 1.2f).forEach { (text, weight) ->
            row.addView(TextView(this).apply {
                this.text = text
                textSize = if (isHeader) 12f else 14f
                setTextColor(Color.parseColor(when {
                    isHeader -> "#FFD700"
                    text.contains("K") && !text.contains("₹") -> "#FFD700"
                    text.contains("₹") -> "#f0e0a0"
                    else -> "#a08050"
                }))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
                setPadding(12, 10, 12, 10)
            })
        }
        return row
    }

    private fun showError(msg: String) {
        progressBar.visibility = View.GONE
        tvError.text = "⚠ $msg"
        tvError.visibility = View.VISIBLE
    }
}
