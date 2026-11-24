package no.nav.dagpenger.saksbehandling.db.oppgave

import java.util.UUID

internal fun map(sakId: UUID): UUID? =
    mapping.get(sakId.toString())?.let {
        UUID.fromString(it)
    }

private val mapping =
    mapOf(
        // sakId til behandlingId
        "019ad945-2250-747c-9a9b-6f426f8cd199" to "019ad945-220d-7a1d-b5fa-fc9c2367827e",
        "019ad8e9-c550-769d-8858-3dea94520cde" to "019ad8e9-c51d-774a-8ce8-aec59d079457",
        "019ad8c2-b803-7c21-88bd-b22497876fe1" to "019ad8c2-b7d1-7049-8ba7-52d80e827421",
        "019ad765-ea39-7005-8d78-0241c0ffccfc" to "019ad765-ea07-70b8-875a-36044ef3a348",
        "019abb5d-bae1-7fe1-b612-28de04c68eba" to "019abb5d-baad-7bff-849d-2c6cd3942f67",
        "019abb2f-eac3-71ae-90dc-64e8c0cfdc36" to "019abb2f-ea90-7907-84dc-8bc57c224d6c",
        "019ab77a-aa18-753c-bb43-c1c1de03b7bf" to "019ab77a-a98c-724e-ad7f-0ae06bac699a",
        "019ab656-25d1-776c-beb5-2d2ac4dd3321" to "019ab656-2586-7764-a084-54c0448bfaf4",
        "019ab60b-79be-787a-892f-bd71680a0e91" to "019ab60b-798c-782f-aaa0-ec4a7afd62ec",
        "019ab5db-e834-74e6-8bbc-b8aaaa0dcc39" to "019ab5db-e7ee-7839-9e98-27e16e646d0d",
        "019ab5da-8981-7f51-a9c6-6711a43bbd28" to "019ab5da-8945-70b7-aaf8-990a36d82cef",
        "019ab5da-38ec-74a3-982d-e2682f8b2a72" to "019ab5da-38c0-7849-b298-b51e0cdaebed",
        "019ab520-50cd-7113-9d9a-ea68fd227a34" to "019ab520-5097-76af-b295-68e35b765d77",
        "019ab51c-3cf9-7f13-9cc1-3f8590977cdc" to "019ab51c-3cbd-7610-bdf1-f12ae20eb6f0",
        "019ab2f4-589f-7d63-8b20-be7e73248a0d" to "019ab2f4-5863-729b-8bb3-b9c6e3ba952d",
        "019aa5c1-350e-73cd-ae01-44b7b162c0cb" to "019aa5c1-34d3-7119-bd43-1b395af65f92",
        "019aa274-de0c-71c1-a2d9-3cdd79fdda96" to "019aa274-ddd4-7bff-80bb-24956c75b842",
        "019aa1b1-a21a-7ca7-83e6-807287df1eb1" to "019aa1b1-a1f6-756f-af1c-9423518588b5",
        "019aa1aa-5116-7ae4-bc14-932afa3d96ac" to "019aa1aa-50e9-7210-b522-a01f19cdf656",
        "019aa07c-607e-7a9f-ad7f-5b0b4b08a340" to "019aa07c-6022-7ffe-a136-ca23b600a718",
        "019a9cc3-0320-768e-9f0a-2be9ad1b6880" to "019a9cc3-02e5-7f51-8a2e-dad74b6e2da4",
        "019a9aeb-7c91-7e8f-bb1d-f40de8af7275" to "019a9aeb-7c51-7983-bdc1-4f5a8e4cbd5d",
        "019a9611-db54-738b-a074-dd23241f5953" to "019a9611-db2a-796c-a84f-1897cf644e7f",
        "019a912d-5828-7bae-9cae-c1520013b4c6" to "019a912d-57a3-77dd-aace-8ecf6d2d502c",
        "019a9124-1837-7ba0-b13d-15ea7577ff08" to "019a9124-17c5-7bf0-88f0-8264191d5a2c",
        "019a9113-a7b5-7fe2-979c-4c322a932d79" to "019a9113-a755-7ee1-a805-5bcad28d4e61",
        "019a8299-a1dd-7cba-bd5b-8302d15bb3e4" to "019a8299-a1a2-70bf-a7fc-46a5b5cc00ef",
        "019a7d6e-cb8c-7a3a-aa37-222d60dabe9e" to "019a7d6e-cb3f-7471-8dc2-76b014f24bc7",
        "019a7d59-185b-7d08-bd47-4930c498b375" to "019a7d59-182d-76a4-952a-26268218cc5e",
        "019a7d1e-f2cb-7132-9671-bf2d40983d91" to "019a7d1e-f209-7108-ab8f-094ad89fac84",
        "019a7cf1-b82b-7aa3-8e97-3b04bb1ce9cf" to "019a7cf1-b7f5-7e3c-b766-bd90a3f764e3",
        "019a7cc3-4fa0-7aea-87b9-382961c55a05" to "019a7cc3-4f71-7ca6-8b2e-c191ce8b270a",
        "019a7c4a-75e3-7a5c-8352-545494a8e73a" to "019a7c4a-7554-79de-a82f-1d5ef65b59b7",
        "019a7760-60e0-7882-9bde-0bc1242ed666" to "019a7760-6078-7a7f-9d5d-7ab291b582db",
        "019a73df-8834-737b-9780-a3a4941781b5" to "019a73df-8806-7dd8-80a5-e70bfeade4f0",
        "019a730c-1554-7fad-a675-64d753ef3267" to "019a730c-152e-7486-b0e5-e6b0443efb15",
        "019a72fe-cbfa-7d26-9d39-c7477451258f" to "019a72fe-cbcf-7587-83e3-9e23f9cfe9e6",
        "019a72cf-ef1a-7d2b-8a6d-2983bdf95c1f" to "019a72cf-eeee-706b-8232-f662d8fd4a4d",
        "019a72a8-5980-7e6c-91df-e0080f43d38d" to "019a72a8-592b-7130-9995-92a6829e3a12",
        "019a6cfe-2fdb-7f9f-8584-a97597cf46a1" to "019a6cfe-2d01-790b-8645-6dc04959ce64",
        "019a6cfe-2c55-7b92-959d-a42474f26b99" to "019a6cfe-2aca-7dd2-88ac-31cc8d28d925",
        "019a6cf8-0422-7582-9915-ca002f6e04b2" to "019a6cf8-0165-71af-b7fb-528082e61d9f",
        "019a59e0-647d-7dfd-bcbf-045f59d54d2f" to "019a59e0-641b-7951-83b3-5a74ae955ee0",
        "019a58f3-ee6b-7ec5-84d8-4f195c13a66e" to "019a58f3-ee41-7235-a97c-2c073442227a",
        "019a5355-6956-7b88-8117-2d0877af7c74" to "019a5355-6907-7dc5-8408-1803782c3431",
        "019a493f-af19-7502-82a2-b18fb275a877" to "019a493f-aea4-7c61-b1fa-904274bf378a",
        "01993b0b-a3c7-7686-ac0e-526cfe61f2b9" to "01993b0b-a38d-7a3a-9909-6e089ae78a06",
        "01992dbd-581a-74e1-978f-2977709c9fa1" to "01992dbd-57ce-7b2e-b109-ca060a42b696",
        "01992b10-a692-70b6-8a5c-0013332e11a8" to "01992b10-a667-7d6f-b417-a1aaadd4b89b",
        "019928dc-f552-77a9-a19e-3ce27eea97b8" to "019928dc-f521-7723-8ff6-f07154f5097d",
    )
