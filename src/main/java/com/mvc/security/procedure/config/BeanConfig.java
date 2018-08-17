package com.mvc.security.procedure.config;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.http.HttpService;
import org.web3j.quorum.Quorum;

import java.io.IOException;
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

}
