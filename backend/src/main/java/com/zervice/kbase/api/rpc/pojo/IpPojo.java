package com.zervice.kbase.api.rpc.pojo;

import com.maxmind.geoip2.model.CityResponse;
import lombok.*;

/**
 * parse from maxmind ip res
 * @author chen
 * @date 2022/10/28
 */
@Builder
@ToString
@Setter@Getter
@AllArgsConstructor
@NoArgsConstructor
public class IpPojo {

    private static final String _UNKNOWN = "unknown";

    public static IpPojo empty(String ip) {
        return IpPojo.builder()
                .ip(ip)
                .country(_UNKNOWN)
                .province(_UNKNOWN)
                .city(_UNKNOWN)
                .latitude(0.0)
                .longitude(0.0)
                .build();
    }

    public static IpPojo parse(@NonNull CityResponse cityResponse) {
        String ip = cityResponse.getTraits().getIpAddress();
        String country = cityResponse.getCountry().getName();
        String province = cityResponse.getSubdivisions().get(0).getName();
        String city = cityResponse.getCity().getName();
        Double latitude = cityResponse.getLocation().getLatitude();
        Double longitude = cityResponse.getLocation().getLongitude();


        return IpPojo.builder()
                .ip(ip)
                .country(country)
                .province(province)
                .city(city)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    private String _ip;

    private String _country;
    private String _province;
    private String _city;
    private Double _latitude;
    private Double _longitude;

}
