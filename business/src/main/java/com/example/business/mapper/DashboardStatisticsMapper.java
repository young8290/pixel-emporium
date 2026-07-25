package com.example.business.mapper;

import com.example.business.response.DailyStatisticsResponse;
import com.example.business.response.HotProductResponse;
import com.example.business.response.RecentOrderResponse;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

public interface DashboardStatisticsMapper {
    @Select("select count(*) from `order` where deleted = 0")
    long selectTotalOrderCount();

    @Select("select count(*) from `order` where deleted = 0 and created_at >= curdate() "
            + "and created_at < date_add(curdate(), interval 1 day)")
    long selectTodayOrderCount();

    @Select("select coalesce(sum(pay_amount), 0) from `order` where deleted = 0 "
            + "and status in ('PAID', 'SHIPPED', 'COMPLETED') and paid_at >= curdate() "
            + "and paid_at < date_add(curdate(), interval 1 day)")
    BigDecimal selectTodayPaidSales();

    @Select("select count(*) from product where deleted = 0 and on_sale = 1")
    long selectOnSaleProductCount();

    @Select("""
            with recursive days(day) as (
                select curdate() - interval 6 day
                union all
                select day + interval 1 day from days where day < curdate()
            )
            select date_format(days.day, '%Y-%m-%d') as day,
                   count(o.id) as order_count,
                   coalesce(sum(case when o.status in ('PAID', 'SHIPPED', 'COMPLETED')
                                     then o.pay_amount else 0 end), 0) as paid_sales
              from days
              left join `order` o on o.deleted = 0
                 and o.created_at >= days.day
                 and o.created_at < date_add(days.day, interval 1 day)
             group by days.day
             order by days.day
            """)
    List<DailyStatisticsResponse> selectSevenDayTrend();

    @Select("""
            select oi.product_id, max(oi.product_title) as product_title, max(oi.image) as image,
                   coalesce(sum(oi.quantity), 0) as sold_quantity,
                   coalesce(sum(oi.price * oi.quantity), 0) as paid_amount
              from order_item oi
              join `order` o on o.id = oi.order_id
             where oi.deleted = 0 and o.deleted = 0
               and o.status in ('PAID', 'SHIPPED', 'COMPLETED')
             group by oi.product_id
             order by sold_quantity desc, paid_amount desc, oi.product_id asc
             limit 5
            """)
    List<HotProductResponse> selectHotProducts();

    @Select("""
            select order_no, status, pay_amount, created_at
              from `order`
             where deleted = 0
             order by created_at desc, id desc
             limit 10
            """)
    List<RecentOrderResponse> selectRecentOrders();
}
