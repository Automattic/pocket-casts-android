package au.com.shiftyjelly.pocketcasts.repositories.di

import javax.inject.Qualifier

/**
 * Annotation for providing the Call.Factory used for downloads. The provides method
 * for this annotation must be provided in the relevant application module because
 * the Call.Factory is different for Wear.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadCallFactory

/**
 * Annotation for providing the OkhttpClient for download calls.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadOkHttpClient

/**
 * Annotation for providing the Request.Builder used for download calls. The provides method
 * for this annotation must be provided in the relevant application module because
 * the Request.Builder is different for Wear.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadRequestBuilder
