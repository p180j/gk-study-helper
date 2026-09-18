package com.gkstudy.mockexam.controller;
import com.gkstudy.common.ApiResponse; import com.gkstudy.mockexam.dto.*; import com.gkstudy.mockexam.model.*; import com.gkstudy.mockexam.service.MockExamService; import org.springframework.web.bind.annotation.*; import javax.validation.Valid; import java.util.List;
@RestController @RequestMapping("/api/mock")
public class MockExamController {
 private final MockExamService service; public MockExamController(MockExamService service){this.service=service;}
 @GetMapping("/papers") public ApiResponse<List<MockPaper>> papers(){return ApiResponse.success(service.papers());}
 @GetMapping("/papers/{id}") public ApiResponse<MockPaper> paper(@PathVariable Long id){return ApiResponse.success(service.paper(id));}
 @PostMapping("/papers/{id}/sessions") public ApiResponse<MockSessionView> start(@RequestHeader(value="X-User-Id",defaultValue="1") Long userId,@PathVariable Long id){return ApiResponse.success(service.start(userId,id));}
 @GetMapping("/sessions/{id}") public ApiResponse<MockSessionView> session(@RequestHeader(value="X-User-Id",defaultValue="1") Long userId,@PathVariable Long id){return ApiResponse.success(service.session(userId,id));}
 @PostMapping("/sessions/{id}/answers") public ApiResponse<MockAnswer> answer(@RequestHeader(value="X-User-Id",defaultValue="1") Long userId,@PathVariable Long id,@Valid @RequestBody MockAnswerRequest request){return ApiResponse.success(service.answer(userId,id,request,false));}
 @PostMapping("/sessions/{id}/skip") public ApiResponse<MockAnswer> skip(@RequestHeader(value="X-User-Id",defaultValue="1") Long userId,@PathVariable Long id,@Valid @RequestBody MockAnswerRequest request){return ApiResponse.success(service.answer(userId,id,request,true));}
 @PostMapping("/sessions/{id}/submit") public ApiResponse<MockResult> submit(@RequestHeader(value="X-User-Id",defaultValue="1") Long userId,@PathVariable Long id){return ApiResponse.success(service.submit(userId,id));}
 @GetMapping("/sessions/{id}/result") public ApiResponse<MockResult> result(@RequestHeader(value="X-User-Id",defaultValue="1") Long userId,@PathVariable Long id){return ApiResponse.success(service.result(userId,id));}
 @GetMapping("/history") public ApiResponse<List<MockSession>> history(@RequestHeader(value="X-User-Id",defaultValue="1") Long userId){return ApiResponse.success(service.history(userId));}
 @GetMapping("/performance") public ApiResponse<List<MockResult>> performance(@RequestHeader(value="X-User-Id",defaultValue="1") Long userId){return ApiResponse.success(service.performance(userId));}
}
