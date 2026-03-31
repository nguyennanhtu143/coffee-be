package org.example.coffee.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.example.coffee.dto.statistic.RevenueOutput;
import org.example.coffee.dto.statistic.StatisticOverviewOutput;
import org.example.coffee.service.StatisticService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/statistic")
public class StatisticController {
    private final StatisticService statisticService;

    @Operation(summary = "Tổng quan thống kê shop")
    @GetMapping("/overview")
    public StatisticOverviewOutput getOverview(@RequestHeader("Authorization") String accessToken) {
        return statisticService.getOverview(accessToken);
    }

    @Operation(summary = "Doanh thu theo ngày (mặc định 30 ngày gần nhất)")
    @GetMapping("/revenue-by-days")
    public List<RevenueOutput> getRevenueByDays(@RequestHeader("Authorization") String accessToken,
                                                 @RequestParam(defaultValue = "30") int days) {
        return statisticService.getRevenueByDays(accessToken, days);
    }

    @Operation(summary = "Doanh thu theo tháng (mặc định 12 tháng gần nhất)")
    @GetMapping("/revenue-by-months")
    public List<RevenueOutput> getRevenueByMonths(@RequestHeader("Authorization") String accessToken,
                                                   @RequestParam(defaultValue = "12") int months) {
        return statisticService.getRevenueByMonths(accessToken, months);
    }
}
