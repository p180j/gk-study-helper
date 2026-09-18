package com.gkstudy.mockexam.controller;
import com.gkstudy.common.*; import com.gkstudy.mockexam.model.*; import com.gkstudy.mockexam.service.MockExamService; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/admin/mock-papers")
public class AdminMockExamController {
 private final MockExamService service; public AdminMockExamController(MockExamService service){this.service=service;}
 @GetMapping public ApiResponse<List<MockPaper>> list(){return ApiResponse.success(service.adminPapers());}
 @GetMapping("/{id}") public ApiResponse<MockPaper> detail(@PathVariable Long id){return ApiResponse.success(service.paper(id));}
 @PostMapping public ApiResponse<MockPaper> create(@RequestBody MockPaper paper){return ApiResponse.success(service.createPaper(paper));}
 @PostMapping("/{id}/status") public ApiResponse<MockPaper> status(@PathVariable Long id,@RequestBody Map<String,String> body){return ApiResponse.success(service.changeStatus(id,body.get("status")));}
}
