package com.mvc.security.procedure.config;

import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.neemre.btcdcli4j.core.client.BtcdClient;
import com.neemre.btcdcli4j.core.client.BtcdClientImpl;
import lombok.Cleanup;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * beanconfig
 *
 * @author qiyichen
 * @create 2018/3/8 19:53
 */
@Configuration
public class BeanConfig {


    @Bean
    public OkHttpClient okHttpClient() throws IOException {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request originalRequest = chain.request();
                        Request requestWithUserAgent = originalRequest.newBuilder()
                                .build();
                        Response result = null;
                        try {
                            result = chain.proceed(requestWithUserAgent);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                        return result;
                    }
                });
        return builder.build();
    }

    @Bean
    public BtcdClient btcdClient() throws IOException, BitcoindException, CommunicationException {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        CloseableHttpClient httpProvider = HttpClients.custom().setConnectionManager(cm).build();
        Properties nodeConfig = new Properties();

        String filePath = System.getProperty("user.dir")
                + "/application.yml";
        @Cleanup InputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(filePath));
        } catch (FileNotFoundException e) {
            ClassPathResource resource = new ClassPathResource("application.yml");
            inputStream = resource.getInputStream();
        }
        nodeConfig.load(inputStream);
        return new BtcdClientImpl(httpProvider, nodeConfig);
    }

}
