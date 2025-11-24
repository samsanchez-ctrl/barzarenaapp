package cl.samuel.barzarena.viewmodel

import cl.samuel.barzarena.data.local.SessionManager
import cl.samuel.barzarena.data.repository.BetRepository
import cl.samuel.barzarena.data.repository.ItemRepository
import cl.samuel.barzarena.data.repository.UserRepository
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    lateinit var userRepository: UserRepository

    @RelaxedMockK
    lateinit var betRepository: BetRepository

    @RelaxedMockK
    lateinit var itemRepository: ItemRepository

    @RelaxedMockK
    lateinit var sessionManager: SessionManager

    private lateinit var viewModel: MainViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // GIVEN: Le digo a mi mock que NO hay sesión guardada al iniciar.
        // Esto es CRUCIAL para que el init() del ViewModel no falle.
        every { sessionManager.getUserId() } returns null

        // WHEN: Cree el ViewModel.
        viewModel = MainViewModel(userRepository, betRepository, itemRepository, sessionManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `rechargeBalance should update balance correctly when a valid amount is provided`() = runTest {
        // GIVEN (Dado)
        viewModel.loginSuccess("testUser", 1000, 1)
        val rechargeAmount = 500
        val expectedBalance = 1500

        // WHEN (Cuando)
        var successResult = false
        viewModel.rechargeBalance(rechargeAmount) { success ->
            successResult = success
        }

        // Con StandardTestDispatcher, la corrutina se ha puesto en cola pero no ha corrido.
        assertEquals(1000, viewModel.balance)

        // AHORA, le decimos a la prueba que ejecute todo lo que está pendiente.
        advanceUntilIdle()

        // THEN (Entonces)
        // Ahora que el trabajo ha terminado, el saldo debe ser el correcto.
        assertEquals(expectedBalance, viewModel.balance)
        assertEquals(true, successResult)
    }
}
