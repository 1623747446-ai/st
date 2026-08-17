package com.example.protocolsignaturelab;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends Activity {

    // V1 故意把密钥留在 Java 层，后续用于 JADX 静态分析练习。
    private static final String LAB_SECRET = "LAB_SECRET_V1_2026";
    private static final String API_PATH = "/api/follow/add";

    private EditText serverUrl;
    private EditText userId;
    private EditText targetUid;
    private EditText fromPage;
    private TextView logText;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serverUrl = findViewById(R.id.serverUrl);
        userId = findViewById(R.id.userId);
        targetUid = findViewById(R.id.targetUid);
        fromPage = findViewById(R.id.fromPage);
        logText = findViewById(R.id.logText);
        sendButton = findViewById(R.id.sendButton);

        sendButton.setOnClickListener(v -> sendSignedRequest());
    }

    private void sendSignedRequest() {
        sendButton.setEnabled(false);
        logText.setText("正在发送...\n");

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                String baseUrl = serverUrl.getText().toString().trim();
                String uid = userId.getText().toString().trim();
                String target = targetUid.getText().toString().trim();
                String page = fromPage.getText().toString().trim();

                long timestamp = System.currentTimeMillis() / 1000L;

                // 为了让 V1 容易理解，JSON 顺序固定，服务器直接对“原始 body 字符串”验签。
                String body =
                        "{"
                                + "\"user_id\":" + uid + ","
                                + "\"follow_user_id\":\"" + escapeJson(target) + "\","
                                + "\"from_page\":\"" + escapeJson(page) + "\","
                                + "\"timestamp\":" + timestamp
                                + "}";

                String canonical =
                        "POST\n"
                                + API_PATH + "\n"
                                + timestamp + "\n"
                                + body;

                String signature = hmacSha256Hex(LAB_SECRET, canonical);
                String authorization = "LAB " + signature;

                URL url = new URL(trimTrailingSlash(baseUrl) + API_PATH);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setDoOutput(true);

                connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                connection.setRequestProperty("User-Agent", "ProtocolSignatureLab/1.0");
                connection.setRequestProperty("Timestamp", String.valueOf(timestamp));
                connection.setRequestProperty("Authorization", authorization);

                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(bytes.length);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(bytes);
                }

                int status = connection.getResponseCode();
                InputStream input = status >= 400
                        ? connection.getErrorStream()
                        : connection.getInputStream();

                String response = readAll(input);

                String output =
                        "POST " + API_PATH + "\n"
                                + "Timestamp: " + timestamp + "\n"
                                + "Authorization: " + authorization + "\n"
                                + "Content-Type: application/json;charset=utf-8\n\n"
                                + body + "\n\n"
                                + "HTTP " + status + "\n"
                                + response;

                runOnUiThread(() -> logText.setText(output));

            } catch (Exception e) {
                runOnUiThread(() ->
                        logText.setText("请求失败：\n" + e.getClass().getSimpleName() + ": " + e.getMessage())
                );
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }

                runOnUiThread(() -> sendButton.setEnabled(true));
            }
        }).start();
    }

    private static String hmacSha256Hex(String secret, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec key = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(key);

        byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

        StringBuilder out = new StringBuilder();
        for (byte b : digest) {
            out.append(String.format("%02x", b & 0xff));
        }
        return out.toString();
    }

    private static String readAll(InputStream input) throws Exception {
        if (input == null) return "";

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8)
        );

        StringBuilder result = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            result.append(line);
        }

        return result.toString();
    }

    private static String trimTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
