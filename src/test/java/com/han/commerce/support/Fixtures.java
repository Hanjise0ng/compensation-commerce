package com.han.commerce.support;

import com.han.commerce.common.domain.Money;
import com.han.commerce.coupon.domain.Coupon;
import com.han.commerce.coupon.domain.MemberCoupon;
import com.han.commerce.member.domain.Member;
import com.han.commerce.point.domain.PointWallet;
import com.han.commerce.product.domain.Product;
import com.han.commerce.stock.domain.Stock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class Fixtures {

    private final AtomicLong sequence = new AtomicLong();

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public Long member() {
        Member member = Member.create("member" + sequence.incrementAndGet() + "@test.com", "테스터");
        em.persist(member);
        return member.getId();
    }

    @Transactional
    public Product product(String name, long price, int stockQuantity) {
        Product product = Product.create(name, Money.of(price));
        em.persist(product);

        Stock stock = Stock.create(product.getId());
        em.persist(stock);
        em.persist(stock.receive(stockQuantity));
        return product;
    }

    @Transactional
    public Long fixedCoupon(Long memberId, long discount, long minOrderAmount, LocalDateTime now) {
        Coupon coupon = Coupon.fixed(discount + "원 할인", Money.of(discount), Money.of(minOrderAmount),
                now.minusDays(1), now.plusDays(30));
        em.persist(coupon);

        MemberCoupon memberCoupon = MemberCoupon.issue(memberId, coupon.getId());
        em.persist(memberCoupon);
        return memberCoupon.getId();
    }

    @Transactional
    public void pointWallet(Long memberId, long balance) {
        PointWallet wallet = PointWallet.create(memberId);
        em.persist(wallet);
        em.persist(wallet.charge(Money.of(balance)));
    }

}