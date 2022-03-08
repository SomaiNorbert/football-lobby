package com.example.football_lobby.models

import android.os.Bundle
import androidx.annotation.NonNull

data class NotificationData(@NonNull val title: String,
                            @NonNull val message: String,
                            @NonNull val destination: String,
                            val uid: String = ""
)