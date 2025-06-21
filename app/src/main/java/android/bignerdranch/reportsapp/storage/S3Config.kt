package android.bignerdranch.reportsapp.storage

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client

object S3Config {
    private const val ACCESS_KEY = "vzVJCbL4hkRfK86RXry6eH"
    private const val SECRET_KEY = "4Ew7b3x4oD6uKEALud28xLrxLdRamut3ZdpxP3rtk9Zs"
    const val BUCKET_NAME = "reoprtsapp"
    const val ENDPOINT = "https://hb.ru-msk.vkcloud-storage.ru/" // Пример для VK Cloud

    val credentials = BasicAWSCredentials(ACCESS_KEY, SECRET_KEY)
    val s3Client = AmazonS3Client(credentials).apply {
            setRegion(Region.getRegion(Regions.DEFAULT_REGION))
            endpoint = ENDPOINT
    }
}