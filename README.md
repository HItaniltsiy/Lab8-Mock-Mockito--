# Лаборатори №8: Mockito-оор хамаарлыг орлуулж тестлэх

**Хичээл:** F.CSA313 — Программ хангамжийн чанарын баталгаа ба тест (2026)

**Оюутан:** О. Доржсамбуу

**Код:** B232270071

## Орчны хувилбар

`java -version`:

```text
openjdk version "25.0.2" 2026-01-20 LTS
OpenJDK Runtime Environment Temurin-25.0.2+10 (build 25.0.2+10-LTS)
OpenJDK 64-Bit Server VM Temurin-25.0.2+10 (build 25.0.2+10-LTS, mixed mode, sharing)
```

`mvn -version`:

```text
Access is denied.
Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)
Maven home: C:\Program Files\Apache\Maven\apache-maven-3.9.16
Java version: 25.0.2, vendor: Eclipse Adoptium, runtime: C:\Users\User\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.2.10-hotspot
Default locale: en_US, platform encoding: UTF-8
OS name: "windows 11", version: "10.0", arch: "amd64"
```

Maven хувилбарын командын эхэнд `Access is denied.` гэсэн орчны мөр хэвлэгдсэн боловч Maven хувилбар, Java хувилбар болон бусад мэдээллийг гаргасан.

## Хэрэгжүүлэлт ба тест

`CourseRegistrationService`-ийн repository болон email sender хамаарлыг `MockitoExtension`-оор mock болгож, жинхэнэ өгөгдлийн сан эсвэл SMTP сервер ажиллуулахгүйгээр үйлчилгээний логикийг тусгаарлан шалгав. A1–A7-ийн долоон тест болон A8-ийн гурван параметржүүлсэн мөр нийт **10 тест** ажиллуулна. Эцсийн ногоон гаралт `results/mvn-test.txt` файлд хадгалагдсан.

```text
.
├── .gitignore
├── pom.xml
├── README.md
├── src/main/java/mn/edu/must/sqat/
│   ├── Student.java
│   ├── StudentRepository.java
│   ├── EmailSender.java
│   └── CourseRegistrationService.java
├── src/test/java/mn/edu/must/sqat/CourseRegistrationServiceTest.java
└── results/
    ├── mvn-test.txt
    ├── mvn-test-mutant-1.txt
    ├── mvn-test-mutant-2.txt
    └── mvn-test-mutant-3.txt
```

| № | Тестийн метод | Шалгаж буй зан төлөв | Ашигласан хэрэгсэл |
|---|---|---|---|
| A1 | `activeStudent_registeringReturnsOkAndSavesAndEmails` | Идэвхтэй оюутныг бүртгэж, и-мэйл илгээнэ | `when`, `verify` |
| A2 | `missingStudent_registeringReturnsErrorWithoutSavingOrEmailing` | Оюутан байхгүй бол хадгалахгүй, и-мэйл явуулахгүй | `never`, `verifyNoInteractions` |
| A3 | `inactiveStudent_registeringReturnsErrorWithoutDownstreamCalls` | Идэвхгүй оюутанд и-мэйл болон дараагийн repository алхам хүрэхгүй | `verifyNoInteractions`, `never` |
| A4 | `missingPrerequisite_registeringReturnsErrorWithoutSaving` | Урьдчилсан хичээл дутуу бол хадгалахгүй | `when`, `never` |
| A5 | `emailSenderThrows_registrationStillReturnsOkAndSaves` | Имэйл илгээгч runtime exception шидсэн ч бүртгэл амжилттай | `doThrow`, `verify` |
| A6 | `nullArgument_registerThrowsWithoutDependencyInteractions` | Хоёр аргументын аль нэг `null` бол exception шидэж, хамааралд хүрэхгүй | `assertThrows` |
| A7 | `successfulRegistration_emailBodyContainsCourseCode` | Имэйлийн биед хичээлийн код байна | `ArgumentCaptor` |
| A8 | `errorScenario_registeringReturnsExpectedError` | Оюутан/хичээл олдохгүй болон prerequisite дутуу үед зөв алдаа буцна | `@ParameterizedTest`, `@CsvSource` (3 мөр) |

A8-д параметр бүр өөр алдааны салаагаар ажилладаг учраас зарим мөрөнд зарласан stub огт дуудагдахгүй. Mockito-ийн strict stubs горим ийм ашиглагдаагүй stub-д `UnnecessaryStubbingException` үүсгэх тул зөвхөн параметржүүлсэн тестийн stub-үүдэд `lenient()` хэрэглэв.

## Хэсэг B — мутацийн шалгалт

| Мутант | Зориуд хийсэн өөрчлөлт | Унасан тест | Гаралт |
|---|---|---|---|
| 1 | `!student.active()` шалгалтыг авч хаясан | `inactiveStudent_registeringReturnsErrorWithoutDownstreamCalls` | `results/mvn-test-mutant-1.txt` — 10 тестээс 1 failure, `BUILD FAILURE` |
| 2 | `repository.saveRegistration(...)` дуудалтыг авч хаясан | `activeStudent_registeringReturnsOkAndSavesAndEmails`; `emailSenderThrows_registrationStillReturnsOkAndSaves` | `results/mvn-test-mutant-2.txt` — 10 тестээс 2 failure, `BUILD FAILURE` |
| 3 | эхний нөхцөлөөс `|| courseId == null` хэсгийг авч хаясан | `nullArgument_registerThrowsWithoutDependencyInteractions` | `results/mvn-test-mutant-3.txt` — 10 тестээс 1 failure, `BUILD FAILURE` |

Мутаци бүрийн дараа эх кодыг сэргээсэн; эцсийн `src/main` нь starter төслийн эх хувилбартай таарна.

**Мутант 1.** Идэвхгүй оюутны шалгалтыг хасахад A3 `ERROR_INACTIVE_STUDENT` хүлээж байсан ч `ERROR_NO_COURSE` авсан. Учир нь энэ тестийн mock хичээл байгаа эсэхийг stub хийгээгүй тул Mockito-ийн анхдагч `false` утгаар дараагийн салаа ажилласан.

**Мутант 2.** `saveRegistration`-ийг хасахад A1 болон A5 хадгалалтын дуудлагыг `verify` хийдэг тул хоёул `Wanted but not invoked` алдаагаар унасан.

**Мутант 3.** A6 хоёр тусдаа `assertThrows`-оор `studentId == null` болон `courseId == null` тохиолдлыг шалгадаг. Нөхцөлөөс `courseId == null`-ийг авахад эхний шалгалт тэнцсэн боловч хоёр дахь нь exception аваагүй тул A6 унасан; энэ нь хоёр null оролт хоёулаа шалгагдсаныг батална.

## Дүгнэлт

Mock нь Mockito зэрэг framework-аар үүсэж, тухайн тестэд тохируулсан хариу болон дуудлагын бүртгэлийг өгдөг объект юм. Харин fake нь жинхэнэ интерфейсийн хялбаршуулсан ажилладаг хувилбар бөгөөд, жишээ нь өгөгдлийг санах ойд хадгалж болно. Үйлчилгээний логик, энгийн domain object болон утгын объектуудыг mock хийхгүй, харин гадаад системтэй харилцах repository, email sender зэрэг хамаарлыг mock хийх нь тохиромжтой. Учир нь логикийг өөрийг нь mock хийвэл тест бодит кодын үр дүнг шалгахгүй болно. `verify`-г хэрэгтэй бизнесийн харилцан үйлдлийг батлах хэмжээнд хэрэглэв. Дотоод хэрэгжүүлэлтийн дуудлага бүрийг хэт нарийн verify хийвэл кодын үр дүнг өөрчлөхгүй бүтцийн засвар хийхэд тест эвдэрч, кодын зорилгоос илүү бүтэцтэй нь холбогдоно. Тиймээс энэ лабораторид үр дүн, хадгалалт болон и-мэйлтэй холбоотой шаардлагатай харилцан үйлдлийг шалгасан.
