"""
Design a locker system like Amazon Locker where delivery drivers can deposit packages and customers can pick them up using a code.

1. primary capabilities
2. error handling
3. scope boundaries

Primary capabilities:

- Are there different sized compartments? 
- How does the customer get their code? Do we need to send an SMS or email?

Error handling:

- Can one customer have multiple packages in the system at once? Are access tokens unique per package?
- How long do the codes last? What happens to a package if it's never picked up?
- What if all compartments of a given size are full when a driver tries to deposit?

Scope boundaries:

- What's in scope for this system? Are we modeling the whole delivery flow, or just the piece from when the driver arrives at the locker until the customer picks up?


Requirements:
1. Carrier deposits a package by specifying size (small, medium, large)
   - System assigns an available compartment of matching size
   - Opens compartment and returns access token, or error if no space
2. Upon successful deposit, an access token is generated and returned
   - One access token per package
3. User retrieves package by entering access token
   - System validates code and opens compartment
   - Throws specific error if code is invalid or expired
4. Access tokens expire after 7 days
   - Expired codes are rejected if used for pickup
   - Package remains in compartment until staff removes it
5. Staff can open all expired compartments to manually handle packages
   - System opens all compartments with expired tokens
   - Staff physically removes packages and returns them to sender

Out of scope:
- How the package gets to the locker (delivery logistics)
- How the access token reaches the customer (SMS/email notification)

Entities:
- Compartments
- Locker
- AccessToken
"""


class Locker:
    - compartments: Compartments[]
    - accessTokensMapping: Map<string, AccessTokens>

    + depositPackage(size) -> string | error
    + pickup(string) -> void | error
    + openExpiredCompartments() -> void


class AccessToken:
    - code: string
    - expiration: timestamp
    - compartment: Compartment

    + isExpired() -> boolean
    + getCompartment() -> Compartment
    + getCode() -> string 


class Compartment: 
    - size: Size # SMALL, MEDIUM, LARGE
    - occupied: boolean

    + isOccupied() -> boolean
    + markOccupied() -> void
    + markFree() -> void 
    + open() -> void 
    + getSize() -> Size


# Implementation 
# 1. Define the core logic
# 2. Consider the edge cases 

class Locker:
    depositPackage(size):
        """
        Core logic:
        1. Find compartment of the right size
        2. Open the commpartment
        3. Mark this compartment as occupied 
        4. Generating an accessToken
        5. Store the accessToken
        6. Return the access token code 

        Edge cases:
        1. No compartment of right size -> throw error
        """
        compartment = getAvailableCompartment(size)
        if compartment == null
            throw Error("No available compartment of that size")
        compartment.open()
        compartment.markOccupied()
        accessToken = generateAccessToken(compartment)
        code = accesstoken.getCode()
        accessTokensMapping[code] = accessToken 

        return code


    private getAvailableCompartment(size)
        """
        Core logic:
        1. Scan all compartments
        2. For each, see if right size and free, 
        3. First that matches, return it

        Edge cases:
        1. none available -> return null
        """
        for c in compartments
            if c.getSize() == size && !c.isOccupied()
                return c 
        return null

    
    pickup(code: string)
        """
        Core logic:
        1. Look up the code to get accessToken
        2. get the compartment
        3. open the compartment
        4. mark the compartment as free
        5. remove accessCode from the map

        Edge cases:
        1. validation of the code itself, not empty -> throw
        2. Code is not in mapping -> throw
        3. accessToken is expired -> throw
        """
        if code.isEmpty() 
            throw Error("invalid code")

        accessToken = accessTokensMapping[code]

        if accessToken == null
            throw Error("invalid code")

        if accessToken.isExpired()
            throw Error("access code expired")

        compartment = accessToken.getCompartment()
        compartment.open()
        compartmnet.markFree()
        accessTokensMapping.remove(code)


    openExpiredCompartments()
        """
        Core logic:
        1. scan through all access tokens in mapping
        2. find the expired
        3. get the compartment of each
        4. open those compartment
        --- remove the packes
        5. mark the compartment available again
        6. remove acesscode from map??? decision: no

        Corner cases:
        - none that aren't implicitly handled
        """
        for code, accessToken in accessTokensMapping
            if accessToken.isExpired()
                comparment = accessToken.getCompartment()
                compartment.open()
                # assume the employee removed them and doors auto-close
                compartment.markFree()
                # not remove from accessTokenMapping so they still get correct error

            # Loop to clear out the accessToken from the map that are 3month+ old.


# Extensions
"""
1. "What if we want to allow a smaller package to use a larger compartment as a fallback when all exact-size compartments are full?"
2. "How would you handle compartments that are broken or under maintenance?"
3. "How would you ensure packages are actually deposited before generating access tokens?"
"""

# "What if we want to allow a smaller package to use a larger compartment as a fallback when all exact-size compartments are full?"

private getAvailableCompartment(requestedSize)
    sizesInOrder = [SMALL, MEDIUM, LARGE]
    startIdex = sizesInOrder.indexOf(requestedSize)

    for i from startIndex to sizesInOrder.length
        size = sizesInOrder[i]
        for c in compartments
            if c.getSize() == size && !c.isOccupied()
                return c 

    return null


# "How would you handle compartments that are broken or under maintenance?"


enum CompartmentStatus:
    AVAILABLE
    OCCUPIED
    OUT_OF_SERVICE 


class Compartment:
    - size: Size
    - status: CompartmentStatus

    + isAvailable() -> boolean
    + markOccupied()
    + markAvailable()
    + markOutOfService()


# "How would you ensure packages are actually deposited before generating access tokens?"
# two-phased commit 
# 1. reserveCompartment & confirmDeposit


class Locker:
    + reserveCompartment(size) -> reservationId
    + confirmDeposit(reservationId) -> tokenCode
    + cancelReservation(reservationId) -> void

class Compartment:
    - size: Size
    - status: CompartmentStatus  // AVAILABLE, RESERVED, OCCUPIED, OUT_OF_SERVICE

enum CompartmentStatus:
    AVAILABLE
    RESERVED
    OCCUPIED
    OUT_OF_SERVICE


reserveCompartment(size)
    compartment = getAvailableCompartment(size)
    if compartment == null
        throw Error("No available compartment")

    compartment.markReserved()
    compartment.open()
    reservationId = generateReservationId()
    reservationMapping[reservationId] = compartment

    return reservationId

confirmDeposit(reservationId)
    compartment = reservationMapping[reservationId]
    if compartment == null
        throw Error("Invalid reservation")

    compartment.markOccupied()
    accessToken = generateAccessToken(compartment)
    accessTokenMapping[accessToken.getCode()] = accessToken
    reservationMapping.remove(reservationId)

    return accessToken.getCode()




















