package org.example.coffee.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.example.coffee.dto.statistic.RevenueOutput;
import org.example.coffee.dto.statistic.StatisticOverviewOutput;
import org.example.coffee.service.StatisticService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/statistic")
public class StatisticController {
    private final StatisticService statisticService;

    @Operation(summary = "Tổng quan thống kê shop (lọc theo khoảng thời gian nếu có, null = all-time)")
    @GetMapping("/overview")
    public StatisticOverviewOutput getOverview(
            @RequestHeader("Authorization") String accessToken,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return statisticService.getOverview(accessToken, startDate, endDate);
    }

    @Operation(summary = "Doanh thu theo ngày trong khoảng startDate → endDate (tối đa 6 tháng)")
    @GetMapping("/revenue-by-days")
    public List<RevenueOutput> getRevenueByDays(
            @RequestHeader("Authorization") String accessToken,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return statisticService.getRevenueByDays(accessToken, startDate, endDate);
    }

    @Operation(summary = "Doanh thu theo tháng (mặc định 12 tháng gần nhất)")
    @GetMapping("/revenue-by-months")
    public List<RevenueOutput> getRevenueByMonths(
            @RequestHeader("Authorization") String accessToken,
            @RequestParam(defaultValue = "12") int months) {
        return statisticService.getRevenueByMonths(accessToken, months);
    }
}
