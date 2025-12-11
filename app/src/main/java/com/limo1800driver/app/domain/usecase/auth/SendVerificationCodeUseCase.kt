package com.limo1800driver.app.domain.usecase.auth

import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.auth.AuthData
import com.limo1800driver.app.data.repository.DriverAuthRepository
import javax.inject.Inject

/**
 * Use case for sending verification code
 */
class SendVerificationCodeUseCase @Inject constructor(
    private val authRepository: DriverAuthRepository
) {
    suspend operator fun invoke(
        phoneNumber: String,
        countryCode: String,
        countryShortCode: String
    ): Result<BaseResponse<AuthData>> {
        return authRepository.sendVerificationCode(
            phoneNumber = phoneNumber,
            countryCode = countryCode,
            countryShortCode = countryShortCode
        )
    }
}

