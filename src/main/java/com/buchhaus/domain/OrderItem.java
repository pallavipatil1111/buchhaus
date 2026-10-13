package com.buchhaus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "title_snapshot", nullable = false, length = 160)
    private String titleSnapshot;

    @Column(name = "unit_price_paise", nullable = false)
    private long unitPricePaise;

    @Column(nullable = false)
    private int quantity;

    protected OrderItem() {
    }

    OrderItem(CustomerOrder order, Book book, String titleSnapshot, long unitPricePaise, int quantity) {
        this.order = order;
        this.book = book;
        this.titleSnapshot = titleSnapshot;
        this.unitPricePaise = unitPricePaise;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public Book getBook() {
        return book;
    }

    public String getTitleSnapshot() {
        return titleSnapshot;
    }

    public long getUnitPricePaise() {
        return unitPricePaise;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getLineTotalPaise() {
        return unitPricePaise * quantity;
    }
}
