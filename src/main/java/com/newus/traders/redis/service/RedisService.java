/**
 * @author wheesunglee
 * @create date 2023-10-04 16:10:26
 * @modify date 2023-10-22 13:25:00
 */
package com.newus.traders.redis.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import com.newus.traders.exception.CustomException;
import com.newus.traders.exception.ErrorCode;
import com.newus.traders.payment.service.PaymentService;
import com.newus.traders.product.dto.ProductDto;
import com.newus.traders.product.entity.Product;
import com.newus.traders.product.repository.ProductRepository;
import com.newus.traders.user.entity.RefreshToken;
import com.newus.traders.user.entity.User;
import com.newus.traders.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    public Long getUserId(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return user.getUserId();
    }

    public ValueOperations<String, String> operationsForValue() {
        return redisTemplate.opsForValue();
    }

    public Set<String> getKeys(String key) {
        return redisTemplate.keys(key);
    }

    public void deleteKey(String key) {
        redisTemplate.delete(key);
    }

    public void addLikes(Long productId, String username) {

        String productKey = "productId:" + productId;

        if (checkIfLiked(productId, username)) {
            removeLikes(productId, username);
            return;
        }
        operationsForValue().setBit(productKey, getUserId(username), true);
    }

    public void removeLikes(Long productId, String username) {
        String productKey = "productId:" + productId;

        operationsForValue().setBit(productKey, getUserId(username), false);
    }

    public boolean checkIfLiked(Long productId, String username) {
        String productKey = "productId:" + productId;

        return operationsForValue().getBit(productKey, getUserId(username));
    }

    public Long countLikes(Long productId) {

        String productKey = "productId:" + productId;

        Object objectCount = redisTemplate.execute(connection -> {
            return connection.bitCount(productKey.getBytes());
        }, true);

        return (Long) objectCount;
    }

    public List<Boolean> getWeeklyAttendance(LocalDate currentDate, String username) {
        LocalDate startDate = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<Boolean> weeklyAttendance = new ArrayList<>();

        for (int i = 0; i < 5; i++) {

            weeklyAttendance.add(checkAttendance(startDate.plusDays(i), username));

        }

        return weeklyAttendance;
    }

    public boolean allTrue(List<Boolean> list) {
        return list.stream().allMatch(element -> element);
    }

    public String updateAttendance(LocalDate currentDate, String username) {
        if (currentDate.getDayOfWeek() == DayOfWeek.SATURDAY
                || currentDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return "주말은 출첵 이벤트 참여 불가";
        }
        String dateKey = currentDate.toString();

        if (checkAttendance(currentDate, username)) {
            return "오늘은 이미 출석체크를 완료함";
        }

        operationsForValue().setBit(dateKey, getUserId(username), true);
        if (currentDate.getDayOfWeek() == DayOfWeek.FRIDAY) {
            if (allTrue(getWeeklyAttendance(currentDate, username))) {
                paymentService.addBalanceForAttendance(username, 100);
                return "5일 연속 출석체크 - 100 지급!";
            }

        }

        paymentService.addBalanceForAttendance(username, 50);
        return "오늘 출석체크 완료 50 지급!!!!!";
    }

    public boolean checkAttendance(LocalDate currentDate, String username) {

        String dateKey = currentDate.toString();

        return operationsForValue().getBit(dateKey, getUserId(username));
    }

    // @Scheduled(fixedRate = 24 * 60 * 60 * 1000)
    // @Scheduled(fixedRate = 10 * 60 * 1000)
    public void updateLikesInDB() {

        Set<String> productKeySet = redisTemplate.keys("productId*");

        if (productKeySet.isEmpty()) {
            System.out.println("ProductKeySet is Empty");
            return;
        }

        Iterator<String> it = productKeySet.iterator();
        while (it.hasNext()) {

            String productKey = it.next();
            Long productId = Long.parseLong(productKey.split(":")[1]);
            System.out.println(
                    "::::::::::::::::::::::::::::updateLikesInDB 상품번호:" + productId + "::::::::::::::::::::::::::::");

            if (countLikes(productId) != null) {

                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

                System.out.println(
                        "::::::::::::::::::::::::::::updateLikesInDB 좋아요 :" + countLikes(productId)
                                + "::::::::::::::::::::::::::::");

                product.setLikes((Long) countLikes(productId));
                productRepository.save(product);

            }

        }
        System.out.println("likes updated");

    }

    public void saveRefreshToken(RefreshToken refreshToken) {
        operationsForValue()
                .set("RT:" + refreshToken.getKey(), refreshToken.getValue(), refreshToken.getExpiration(),
                        TimeUnit.MILLISECONDS);

    }

    public Object getRefreshToken(String key) {
        Object refreshToken = operationsForValue().get("RT:" + key);
        if (refreshToken == null) {
            throw new RuntimeException("로그아웃 된 사용자입니다.");
        }

        return refreshToken;
    }

    public void deleteKey(Long productId) {

        String productKey = "productId:" + productId;
        operationsForValue().getAndDelete(productKey);
    }

    public List<ProductDto> getMyLikes(String username) {

        Set<String> productKeySet = redisTemplate.keys("productId*");
        List<ProductDto> productDtoList = new ArrayList<>();
        if (productKeySet.isEmpty()) {
            System.out.println("ProductKeySet is Empty");
            return new ArrayList<ProductDto>();
        }

        Iterator<String> it = productKeySet.iterator();
        while (it.hasNext()) {

            String productKey = it.next();
            Long productId = Long.parseLong(productKey.split(":")[1]);

            if (checkIfLiked(productId, username)) {
                productDtoList.add(new ProductDto(productRepository.findById(productId).get()));
            }

        }
        return productDtoList;

    }

}
