package com.example.two_step_auth;


import java.util.Random;

public class OtpGen {
    public static String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}