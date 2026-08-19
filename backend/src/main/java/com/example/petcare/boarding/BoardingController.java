package com.example.petcare.boarding;

import com.example.petcare.common.ApiResponse;
import com.example.petcare.common.PageResult;
import com.example.petcare.order.OrderPayRequest;
import com.example.petcare.order.OrderPayResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/boarding")
public class BoardingController {

    private final BoardingService boardingService;

    public BoardingController(BoardingService boardingService) {
        this.boardingService = boardingService;
    }

    @GetMapping("/areas")
    public ApiResponse<List<BoardingArea>> listAreas() {
        return ApiResponse.success(boardingService.listAreas());
    }

    @PostMapping("/areas")
    public ApiResponse<BoardingArea> createArea(@Valid @RequestBody BoardingAreaRequest request) {
        return ApiResponse.success(boardingService.createArea(request));
    }

    @PutMapping("/areas/{id}")
    public ApiResponse<BoardingArea> updateArea(@PathVariable Long id, @Valid @RequestBody BoardingAreaRequest request) {
        return ApiResponse.success(boardingService.updateArea(id, request));
    }

    @PutMapping("/areas/{id}/status")
    public ApiResponse<BoardingArea> updateAreaStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.success(boardingService.updateAreaStatus(id, status));
    }

    @GetMapping("/locations")
    public ApiResponse<PageResult<BoardingLocation>> listLocations(BoardingLocationQuery query) {
        return ApiResponse.success(boardingService.listLocations(query));
    }

    @PostMapping("/locations")
    public ApiResponse<BoardingLocation> createLocation(@Valid @RequestBody BoardingLocationRequest request) {
        return ApiResponse.success(boardingService.createLocation(request));
    }

    @PutMapping("/locations/{id}")
    public ApiResponse<BoardingLocation> updateLocation(@PathVariable Long id, @Valid @RequestBody BoardingLocationRequest request) {
        return ApiResponse.success(boardingService.updateLocation(id, request));
    }

    @PutMapping("/locations/{id}/status")
    public ApiResponse<BoardingLocation> updateLocationStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.success(boardingService.updateLocationStatus(id, status));
    }

    @PutMapping("/locations/{id}/clean-status")
    public ApiResponse<BoardingLocation> updateCleanStatus(@PathVariable Long id, @RequestParam String cleanStatus) {
        return ApiResponse.success(boardingService.updateCleanStatus(id, cleanStatus));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResult<BoardingOrderView>> listOrders(BoardingOrderQuery query) {
        return ApiResponse.success(boardingService.listOrders(query));
    }

    @PostMapping("/orders")
    public ApiResponse<BoardingOrderView> createOrder(@Valid @RequestBody BoardingOrderRequest request) {
        return ApiResponse.success(boardingService.createOrder(request));
    }

    @PutMapping("/orders/{id}/check-in")
    public ApiResponse<BoardingOrderView> checkIn(@PathVariable Long id) {
        return ApiResponse.success(boardingService.checkIn(id));
    }

    @PutMapping("/orders/{id}/check-out")
    public ApiResponse<BoardingOrderView> checkOut(@PathVariable Long id) {
        return ApiResponse.success(boardingService.checkOut(id));
    }

    @PutMapping("/orders/{id}/picked-up")
    public ApiResponse<BoardingOrderView> confirmPickedUp(@PathVariable Long id) {
        return ApiResponse.success(boardingService.confirmPickedUp(id));
    }

    @PostMapping("/orders/{id}/payment-confirm")
    public ApiResponse<OrderPayResult> confirmPayment(
            @PathVariable Long id,
            @RequestBody(required = false) OrderPayRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(boardingService.confirmPayment(id, request, authorization));
    }

    @PutMapping("/orders/{id}/cancel")
    public ApiResponse<BoardingOrderView> cancelOrder(@PathVariable Long id) {
        return ApiResponse.success(boardingService.cancelOrder(id));
    }

    @PutMapping("/orders/{id}/planned-check-out")
    public ApiResponse<BoardingOrderView> updatePlannedCheckOutTime(
            @PathVariable Long id,
            @Valid @RequestBody BoardingCheckOutTimeRequest request
    ) {
        return ApiResponse.success(boardingService.updatePlannedCheckOutTime(id, request));
    }

    @PutMapping("/orders/{id}/location")
    public ApiResponse<BoardingOrderView> changeLocation(
            @PathVariable Long id,
            @Valid @RequestBody BoardingChangeLocationRequest request
    ) {
        return ApiResponse.success(boardingService.changeLocation(id, request));
    }

    @PutMapping("/orders/{id}/schedule")
    public ApiResponse<BoardingOrderView> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody BoardingScheduleRequest request
    ) {
        return ApiResponse.success(boardingService.updateSchedule(id, request));
    }

    @GetMapping("/room-status")
    public ApiResponse<BoardingRoomStatusView> roomStatus(BoardingRoomStatusQuery query) {
        return ApiResponse.success(boardingService.roomStatus(query));
    }

    @GetMapping("/in-house-pets")
    public ApiResponse<List<BoardingInHousePetView>> inHousePets(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(boardingService.inHousePets(date, areaId, keyword));
    }

    @PostMapping("/care-tasks")
    public ApiResponse<List<BoardingCareTaskView>> createCareTasks(@Valid @RequestBody BoardingCareTaskCreateRequest request) {
        return ApiResponse.success(boardingService.createCareTasks(request));
    }

    @PutMapping("/care-tasks/{id}/complete")
    public ApiResponse<BoardingCareTaskView> completeCareTask(@PathVariable Long id) {
        return ApiResponse.success(boardingService.completeCareTask(id));
    }

    @PutMapping("/care-tasks/{id}/remark")
    public ApiResponse<BoardingCareTaskView> updateCareTaskRemark(
            @PathVariable Long id,
            @Valid @RequestBody BoardingCareTaskRemarkRequest request
    ) {
        return ApiResponse.success(boardingService.updateCareTaskRemark(id, request));
    }
}
