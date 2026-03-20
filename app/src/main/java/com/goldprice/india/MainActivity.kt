package com.goldprice.india

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

data class City(val name: String, val url: String)

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var spinner: Spinner
    private lateinit var btnRefresh: ImageButton

    private val cities = listOf(
        City("India (National)", "https://www.goodreturns.in/gold-rates/"),
        City("Mumbai",           "https://www.goodreturns.in/gold-rates/mumbai.html"),
        City("Delhi",            "https://www.goodreturns.in/gold-rates/delhi.html"),
        City("Bangalore",        "https://www.goodreturns.in/gold-rates/bangalore.html"),
        City("Chennai",          "https://www.goodreturns.in/gold-rates/chennai.html"),
        City("Hyderabad",        "https://www.goodreturns.in/gold-rates/hyderabad.html"),
        City("Pune",             "https://www.goodreturns.in/gold-rates/pune.html"),
        City("Kolkata",          "https://www.goodreturns.in/gold-rates/kolkata.html"),
        City("Ahmedabad",        "https://www.goodreturns.in/gold-rates/ahmedabad.html"),
        City("Jaipur",           "https://www.goodreturns.in/gold-rates/jaipur.html"),
        City("Noida",            "https://www.goodreturns.in/gold-rates/noida.html"),
        City("Lucknow",          "https://www.goodreturns.in/gold-rates/lucknow.html"),
        City("Surat",            "https://www.goodreturns.in/gold-rates/surat.html"),
        City("Vijayawada",       "https://www.goodreturns.in/gold-rates/vijayawada.html"),
        City("Visakhapatnam",    "https://www.goodreturns.in/gold-rates/visakhapatnam.html"),
    )

    private var selectedCityIndex = 0

    // This JS runs after page loads:
    // 1. Hides EVERYTHING on the page
    // 2. Then only shows back the gold price table and heading
    private val cleanupJS = """
        javascript:(function() {

            // --- Step 1: Hide entire body first ---
            document.body.style.visibility = 'hidden';

            // --- Step 2: Apply dark background ---
            document.body.style.background = '#0a0800';
            document.documentElement.style.background = '#0a0800';

            // --- Step 3: Remove all script-injected ads and iframes ---
            document.querySelectorAll('iframe, [id*="ad"], [class*="ad"], [id*="banner"], [class*="banner"], [id*="popup"], [class*="popup"], ins, .widget, aside, header, footer, nav, .breadcrumb, .social, .comment, .related, .newsletter, .subscribe, form').forEach(function(el) {
                el.remove();
            });

            // --- Step 4: Find the gold rate table ---
            // GoodReturns uses a table with class 'gold-rate-table' or similar
            var goldTable = document.querySelector('.gold-rate-table')
                || document.querySelector('table.table')
                || document.querySelector('#gold-rate-content')
                || document.querySelector('.gold_rate_table')
                || document.querySelector('table');

            // --- Step 5: Find the city heading ---
            var heading = document.querySelector('h1')
                || document.querySelector('.page-title')
                || document.querySelector('.heading');

            // --- Step 6: Create a clean wrapper div ---
            var wrapper = document.createElement('div');
            wrapper.style.cssText = [
                'background: #0a0800',
                'color: #f0e0a0',
                'padding: 16px',
                'font-family: Georgia, serif',
                'max-width: 100%',
                'box-sizing: border-box'
            ].join(';');

            // --- Step 7: Add heading ---
            if (heading) {
                var h = document.createElement('h2');
                h.innerText = heading.innerText;
                h.style.cssText = 'color: #FFD700; font-size: 18px; margin-bottom: 12px; text-align: center; border-bottom: 1px solid #3a2800; padding-bottom: 8px;';
                wrapper.appendChild(h);
            }

            // --- Step 8: Style and add the gold table ---
            if (goldTable) {
                goldTable.style.cssText = [
                    'width: 100%',
                    'border-collapse: collapse',
                    'color: #f0e0a0',
                    'font-size: 14px',
                    'background: #1a1200'
                ].join(';');

                // Style all table rows and cells
                goldTable.querySelectorAll('tr').forEach(function(row, i) {
                    row.style.background = (i % 2 === 0) ? '#0f0b00' : '#1a1200';
                    row.querySelectorAll('td, th').forEach(function(cell) {
                        cell.style.cssText = 'padding: 10px 12px; border: 1px solid #3a2800; color: #f0e0a0;';
                    });
                    // Highlight header row
                    row.querySelectorAll('th').forEach(function(th) {
                        th.style.cssText = 'padding: 10px 12px; border: 1px solid #3a2800; color: #FFD700; background: #2a1800; font-weight: bold;';
                    });
                    // Highlight karat column (first column)
                    var firstCell = row.querySelector('td:first-child');
                    if (firstCell) firstCell.style.color = '#FFD700';
                });

                wrapper.appendChild(goldTable);
            } else {
                var msg = document.createElement('p');
                msg.innerText = 'Could not load gold prices. Please refresh.';
                msg.style.color = '#ff9040';
                wrapper.appendChild(msg);
            }

            // --- Step 9: Add "Powered by GoodReturns" credit ---
            var credit = document.createElement('p');
            credit.innerText = 'Source: GoodReturns.in';
            credit.style.cssText = 'color: #504030; font-size: 11px; text-align: center; margin-top: 12px;';
            wrapper.appendChild(credit);

            // --- Step 10: Replace entire body with our clean wrapper ---
            document.body.innerHTML = '';
            document.body.appendChild(wrapper);
            document.body.style.visibility = 'visible';
            document.body.style.margin = '0';
            document.body.style.padding = '0';
            document.body.style.background = '#0a0800';

        })();
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        progressBar = findViewById(R.id.progress_bar)
        spinner = findViewById(R.id.spinner_city)
        btnRefresh = findViewById(R.id.btn_refresh)

        setupWebView()
        setupSpinner()
        setupRefreshButton()
        loadCity(cities[0])
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                return if (url.contains("goodreturns.in")) {
                    false
                } else {
                    true // Block all external links
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                // Inject cleanup JS after page fully loads
                view?.loadUrl(cleanupJS)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.visibility = if (newProgress == 100) View.GONE else View.VISIBLE
            }
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            cities.map { it.name }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != selectedCityIndex) {
                    selectedCityIndex = position
                    loadCity(cities[position])
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRefreshButton() {
        btnRefresh.setOnClickListener { webView.reload() }
    }

    private fun loadCity(city: City) {
        webView.loadUrl(city.url)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
