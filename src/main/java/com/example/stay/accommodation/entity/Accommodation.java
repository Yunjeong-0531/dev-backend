package com.example.stay.accommodation.entity;

import com.example.stay.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "accommodations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Accommodation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 등록한 호스트. auth 의 User 엔티티를 @ManyToOne 으로 엮지 않고 "id 로만" 참조한다.
     * (feature 끼리 엔티티로 결합하지 않기 위한 규칙. DB 외래키는 SQL 에서 걸어도 된다)
     */
    @Column(name = "host_id", nullable = false)
    private Long hostId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String region;

    @Column(name = "price_per_night", nullable = false)
    private int pricePerNight;

    public static Accommodation create(Long hostId, String name, String region, int pricePerNight) {
        Accommodation a = new Accommodation();
        a.hostId = hostId;
        a.name = name;
        a.region = region;
        a.pricePerNight = pricePerNight;
        return a;
    }

    public void update(String name, String region, int pricePerNight) {
        this.name = name;
        this.region = region;
        this.pricePerNight = pricePerNight;
    }

    public boolean isOwnedBy(Long userId) {
        return hostId.equals(userId);
    }
}
