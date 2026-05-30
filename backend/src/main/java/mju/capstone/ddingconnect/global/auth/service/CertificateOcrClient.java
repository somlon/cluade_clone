package mju.capstone.ddingconnect.global.auth.service;

import mju.capstone.ddingconnect.global.auth.dto.response.CertificateVerifyResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * [증명서 OCR 인증 클라이언트]
 *
 * 회원가입 시 업로드된 재학/졸업증명서(PDF)를 데이터 파트({@code ddingconnect-data}) 의
 * {@code POST /api/data/verify} 로 멀티파트 relay 하여 OCR 추출값(이름·학과·학년)을 받아오는
 * 아웃바운드 클라이언트. 커피챗 {@code MatchingAlgorithmClient}/로드맵 {@code RoadmapAiClient}
 * 와 동일한 얇은 {@code RestClient} 패턴이며, 구현체가 엔드포인트·헤더·스키마를 추상화한다.
 */
public interface CertificateOcrClient {

    /**
     * 증명서를 데이터 파트로 전송해 OCR 인증 결과를 받아온다.
     *
     * @param certificate 회원가입 시 업로드된 증명서(PDF) — 같은 바이트를 멀티파트 {@code file} 로 relay
     * @param memberId    저장된 회원 PK — 데이터 파트 인증용 {@code X-User-Id} 헤더로 전달
     * @return 인증 결과(승인 여부 + 추출한 이름·학과·학년)
     */
    CertificateVerifyResponse verify(MultipartFile certificate, Long memberId);
}
