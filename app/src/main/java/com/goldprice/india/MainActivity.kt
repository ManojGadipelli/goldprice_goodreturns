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
import android.widget.TextView
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
        City("Coimbatore",       "https://www.goodreturns.in/gold-rates/coimbatore.html"),
        City("Vijayawada",       "https://www.goodreturns.in/gold-rates/vijayawada.html"),
        City("Visakhapatnam",    "https://www.goodreturns.in/gold-rates/visakhapatnam.html"),
    )

    private var selectedCityIndex = 0

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

        // Load default city
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
                // Keep navigation within goodreturns.in only
                return if (url.contains("goodreturns.in")) {
                    view.loadUrl(url)
                    false
                } else {
                    true // Block external links
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                // Inject CSS to hide ads, header, footer for cleaner look
                val css = """
                    javascript:(function() {
                        var style = document.createElement('style');
                        style.type = 'text/css';
                        style.innerHTML = `
                            header, .header, #header, nav, .nav,
                            .advertisement, .ads, .ad-container, 
                            .sidebar, #sidebar, .widget,
                            footer, .footer, #footer,
                            .breadcrumb, .social-share,
                            .sticky-ad, .popup, .modal,
                            [class*='banner'], [id*='banner'],
                            [class*='advert'], [id*='advert'] {
                                display: none !important;
                            }
                            body { 
                                background: #0a0800 !important; 
                                color: #f0e0a0 !important;
                            }
                            .container, .main-content, #main {
                                padding: 0 !important;
                                margin: 0 !important;
                                max-width: 100% !important;
                            }
                        `;
                        document.head.appendChild(style);
                    })()
                """.trimIndent()
                view?.loadUrl(css)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                } else {
                    progressBar.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupSpinner() {
        val cityNames = cities.map { it.name }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            cityNames
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
        btnRefresh.setOnClickListener {
            webView.reload()
        }
    }

    private fun loadCity(city: City) {
        webView.loadUrl(city.url)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
