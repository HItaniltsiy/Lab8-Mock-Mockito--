package mn.edu.must.sqat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {
    private static final String STUDENT_ID = "S123";
    private static final String COURSE_ID = "CSA313";

    @Mock StudentRepository repository;
    @Mock EmailSender emailSender;
    @Captor ArgumentCaptor<String> bodyCaptor;

    CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    @Test
    void activeStudent_registeringReturnsOkAndSavesAndEmails() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent());
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CSA101"));

        assertEquals("OK", service.register(STUDENT_ID, COURSE_ID));

        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq("student@example.edu"), anyString(), anyString());
    }

    @Test
    void missingStudent_registeringReturnsErrorWithoutSavingOrEmailing() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(null);

        assertEquals("ERROR_NO_STUDENT", service.register(STUDENT_ID, COURSE_ID));

        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    @Test
    void inactiveStudent_registeringReturnsErrorWithoutDownstreamCalls() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(
                new Student(STUDENT_ID, "student@example.edu", false, List.of()));

        assertEquals("ERROR_INACTIVE_STUDENT", service.register(STUDENT_ID, COURSE_ID));

        verifyNoInteractions(emailSender);
        verify(repository, never()).courseExists(anyString());
        verify(repository, never()).prerequisites(anyString());
        verify(repository, never()).saveRegistration(anyString(), anyString());
    }

    @Test
    void missingPrerequisite_registeringReturnsErrorWithoutSaving() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent());
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CSA101", "CSA202"));

        assertEquals("ERROR_PREREQUISITES", service.register(STUDENT_ID, COURSE_ID));

        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    @Test
    void emailSenderThrows_registrationStillReturnsOkAndSaves() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent());
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of());
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        assertEquals("OK", service.register(STUDENT_ID, COURSE_ID));

        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(anyString(), anyString(), anyString());
    }

    @Test
    void nullArgument_registerThrowsWithoutDependencyInteractions() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null, COURSE_ID));
        assertThrows(IllegalArgumentException.class, () -> service.register(STUDENT_ID, null));

        verifyNoInteractions(repository, emailSender);
    }

    @Test
    void successfulRegistration_emailBodyContainsCourseCode() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent());
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of());

        assertEquals("OK", service.register(STUDENT_ID, COURSE_ID));

        verify(emailSender).send(eq("student@example.edu"), anyString(), bodyCaptor.capture());
        org.junit.jupiter.api.Assertions.assertTrue(bodyCaptor.getValue().contains(COURSE_ID));
    }

    @ParameterizedTest(name = "{0} нөхцөлд {1} буцна")
    @CsvSource({
            "no-student, ERROR_NO_STUDENT",
            "no-course, ERROR_NO_COURSE",
            "missing-prerequisite, ERROR_PREREQUISITES"
    })
    void errorScenario_registeringReturnsExpectedError(String scenario, String expected) {
        // Each row uses only its own branch's stubs; unused branch stubs are intentional.
        lenient().when(repository.findStudent(STUDENT_ID)).thenReturn(
                "no-student".equals(scenario) ? null : activeStudent());
        lenient().when(repository.courseExists(COURSE_ID)).thenReturn(!"no-course".equals(scenario));
        lenient().when(repository.prerequisites(COURSE_ID)).thenReturn(
                "missing-prerequisite".equals(scenario) ? List.of("CSA999") : List.of());

        assertEquals(expected, service.register(STUDENT_ID, COURSE_ID));
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    private static Student activeStudent() {
        return new Student(STUDENT_ID, "student@example.edu", true, List.of("CSA101"));
    }
}
