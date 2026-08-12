package org.kkobi.assessment.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MapperXmlTest {

    @Test
    @DisplayName("행동 분석에 사용하는 MyBatis Mapper XML을 파싱한다.")
    void parseMapperXml() throws IOException {
        Configuration configuration = new Configuration();

        parseMapper(
                configuration,
                "org/kkobi/game/mapper/ActionLogMapper.xml"
        );
        parseMapper(
                configuration,
                "org/kkobi/assessment/mapper/AssessmentMapper.xml"
        );
        parseMapper(
                configuration,
                "org/kkobi/assessment/mapper/VirtualInvestmentBehaviorMapper.xml"
        );
        parseMapper(
                configuration,
                "org/kkobi/assessment/mapper/AccountDailySnapshotMapper.xml"
        );
        parseMapper(
                configuration,
                "org/kkobi/assessment/mapper/AssessmentSettlementMapper.xml"
        );

        assertTrue(configuration.hasStatement(
                "org.kkobi.game.mapper.ActionLogMapper.saveActionLog"
        ));
        assertTrue(configuration.hasStatement(
                "org.kkobi.game.mapper.ActionLogMapper.deleteActionLogsByUserId"
        ));
        assertTrue(configuration.hasStatement(
                "org.kkobi.assessment.mapper.AssessmentMapper.saveAssessmentResult"
        ));
        assertTrue(configuration.hasStatement(
                "org.kkobi.game.mapper.ActionLogMapper.existsCompletedGame"
        ));
        assertTrue(configuration.hasStatement(
                "org.kkobi.game.mapper.ActionLogMapper.lockUserById"
        ));
        assertTrue(configuration.hasStatement(
                "org.kkobi.assessment.mapper.VirtualInvestmentBehaviorMapper"
                        + ".getPreviousVirtualInvestmentBehaviors"
        ));
        assertTrue(configuration.hasStatement(
                "org.kkobi.assessment.mapper.VirtualInvestmentBehaviorMapper"
                        + ".getProductBehaviorRequest"
        ));
        assertTrue(configuration.hasStatement(
                "org.kkobi.assessment.mapper.AccountDailySnapshotMapper"
                        + ".saveAccountDailySnapshot"
        ));
        assertTrue(configuration.hasStatement(
                "org.kkobi.assessment.mapper.AssessmentSettlementMapper"
                        + ".saveProcessingAssessmentSettlement"
        ));
        assertTrue(configuration.hasStatement(
                "org.kkobi.assessment.mapper.AssessmentSettlementMapper"
                        + ".restartStaleAssessmentSettlement"
        ));
    }

    private void parseMapper(
            Configuration configuration,
            String mapperResource) throws IOException {
        try (InputStream inputStream = Resources.getResourceAsStream(mapperResource)) {
            XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(
                    inputStream,
                    configuration,
                    mapperResource,
                    configuration.getSqlFragments()
            );
            xmlMapperBuilder.parse();
        }
    }
}
